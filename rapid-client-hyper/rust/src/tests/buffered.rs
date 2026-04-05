use std::convert::Infallible;
use std::net::SocketAddr;
use std::sync::{Arc, Mutex};

use bytes::Bytes;
use http::request::Parts;
use http_body_util::{BodyExt, Full};
use hyper::body::Incoming;
use hyper::server::conn::http1;
use hyper::service::service_fn;
use hyper::{Request, Response};
use hyper_util::rt::TokioIo;
use tokio::net::TcpListener;
use tokio::sync::oneshot;

use super::{fetch_next, new_hyper_client, post_response};
use crate::{
    H_AWS_REQUEST_ID, H_CLIENT_CTX, H_COGNITO, H_DEADLINE_MS, H_FUNCTION_ARN, H_TRACE_ID,
};

struct MockServer {
    addr: SocketAddr,
    requests: Arc<Mutex<Vec<(Parts, Bytes)>>>,
    _shutdown: oneshot::Sender<()>,
}

impl MockServer {
    async fn start(
        handler: impl Fn(&Parts) -> Response<Full<Bytes>> + Send + Sync + 'static,
    ) -> Self {
        let requests: Arc<Mutex<Vec<(Parts, Bytes)>>> = Arc::default();
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let addr = listener.local_addr().unwrap();
        let (tx, mut rx) = oneshot::channel::<()>();
        let handler = Arc::new(handler);
        let reqs = requests.clone();

        tokio::spawn(async move {
            loop {
                tokio::select! {
                    result = listener.accept() => {
                        let (stream, _) = result.unwrap();
                        let handler = handler.clone();
                        let reqs = reqs.clone();
                        tokio::spawn(async move {
                            let io = TokioIo::new(stream);
                            let _ = http1::Builder::new()
                                .serve_connection(
                                    io,
                                    service_fn(move |req: Request<Incoming>| {
                                        let handler = handler.clone();
                                        let reqs = reqs.clone();
                                        async move {
                                            let (parts, body) = req.into_parts();
                                            let body = body.collect().await.unwrap().to_bytes();
                                            let resp = (handler)(&parts);
                                            reqs.lock().unwrap().push((parts, body));
                                            Ok::<_, Infallible>(resp)
                                        }
                                    }),
                                )
                                .await;
                        });
                    }
                    _ = &mut rx => break,
                }
            }
        });

        MockServer { addr, requests, _shutdown: tx }
    }

    fn host(&self) -> String {
        self.addr.to_string()
    }

    fn take_requests(&self) -> Vec<(Parts, Bytes)> {
        std::mem::take(&mut self.requests.lock().unwrap())
    }
}

fn next_ok(
    headers: &[(&str, &str)],
    body: &str,
) -> impl Fn(&Parts) -> Response<Full<Bytes>> + Send + Sync + 'static {
    let headers: Vec<(String, String)> = headers
        .iter()
        .map(|(k, v)| (k.to_string(), v.to_string()))
        .collect();
    let body = Bytes::from(body.to_string());
    move |_| {
        let mut b = Response::builder();
        for (k, v) in &headers {
            b = b.header(k.as_str(), v.as_str());
        }
        b.body(Full::new(body.clone())).unwrap()
    }
}

fn ok_empty() -> impl Fn(&Parts) -> Response<Full<Bytes>> + Send + Sync + 'static {
    |_| Response::new(Full::new(Bytes::new()))
}

// ── fetch_next tests ──────────────────────────────────────────────

#[tokio::test]
async fn next_basic() {
    let server = MockServer::start(next_ok(
        &[
            (H_AWS_REQUEST_ID, "rid-123"),
            (H_FUNCTION_ARN, "arn:aws:lambda:us-east-1:123:function:f"),
        ],
        r#"{"hello":"world"}"#,
    ))
    .await;

    let client = new_hyper_client();
    let result = fetch_next(&client, &server.host()).await.unwrap();

    assert_eq!(result.id, "rid-123");
    assert_eq!(result.arn, "arn:aws:lambda:us-east-1:123:function:f");
    assert_eq!(result.deadline_ms, 0);
    assert!(result.trace_id.is_none());
    assert!(result.client_context.is_none());
    assert!(result.cognito_identity.is_none());
    assert_eq!(&result.body[..], br#"{"hello":"world"}"#);

    let reqs = server.take_requests();
    assert_eq!(reqs.len(), 1);
    let (parts, _) = &reqs[0];
    assert_eq!(parts.uri.path(), "/2018-06-01/runtime/invocation/next");
    assert_eq!(parts.method, http::Method::GET);
    let ua = parts.headers.get("user-agent").unwrap().to_str().unwrap();
    assert!(ua.starts_with("com.smirnoal.crema-hyper/"));
    assert!(ua.contains("crema-hyper"));
}

#[tokio::test]
async fn next_deadline() {
    let server = MockServer::start(next_ok(
        &[
            (H_AWS_REQUEST_ID, "rid"),
            (H_FUNCTION_ARN, "arn"),
            (H_DEADLINE_MS, "5000"),
        ],
        "",
    ))
    .await;

    let client = new_hyper_client();
    let result = fetch_next(&client, &server.host()).await.unwrap();
    assert_eq!(result.deadline_ms, 5000);
}

#[tokio::test]
async fn next_malformed_deadline_falls_back_to_zero() {
    let server = MockServer::start(next_ok(
        &[
            (H_AWS_REQUEST_ID, "rid"),
            (H_FUNCTION_ARN, "arn"),
            (H_DEADLINE_MS, "not-a-number"),
        ],
        "",
    ))
    .await;

    let client = new_hyper_client();
    let result = fetch_next(&client, &server.host()).await.unwrap();
    assert_eq!(result.deadline_ms, 0);
}

#[tokio::test]
async fn next_trace_id() {
    let server = MockServer::start(next_ok(
        &[
            (H_AWS_REQUEST_ID, "rid"),
            (H_FUNCTION_ARN, "arn"),
            (H_TRACE_ID, "Root=1-abc-def"),
        ],
        "",
    ))
    .await;

    let client = new_hyper_client();
    let result = fetch_next(&client, &server.host()).await.unwrap();
    assert_eq!(result.trace_id.as_deref(), Some("Root=1-abc-def"));
}

#[tokio::test]
async fn next_client_context() {
    let server = MockServer::start(next_ok(
        &[
            (H_AWS_REQUEST_ID, "rid"),
            (H_FUNCTION_ARN, "arn"),
            (H_CLIENT_CTX, r#"{"custom":"ctx"}"#),
        ],
        "",
    ))
    .await;

    let client = new_hyper_client();
    let result = fetch_next(&client, &server.host()).await.unwrap();
    assert_eq!(result.client_context.as_deref(), Some(r#"{"custom":"ctx"}"#));
}

#[tokio::test]
async fn next_cognito() {
    let server = MockServer::start(next_ok(
        &[
            (H_AWS_REQUEST_ID, "rid"),
            (H_FUNCTION_ARN, "arn"),
            (H_COGNITO, "cognito-id-123"),
        ],
        "",
    ))
    .await;

    let client = new_hyper_client();
    let result = fetch_next(&client, &server.host()).await.unwrap();
    assert_eq!(result.cognito_identity.as_deref(), Some("cognito-id-123"));
}

#[tokio::test]
async fn next_all_optional_headers() {
    let server = MockServer::start(next_ok(
        &[
            (H_AWS_REQUEST_ID, "rid"),
            (H_FUNCTION_ARN, "arn"),
            (H_DEADLINE_MS, "9999"),
            (H_TRACE_ID, "trace-1"),
            (H_CLIENT_CTX, "ctx-1"),
            (H_COGNITO, "cog-1"),
        ],
        "payload",
    ))
    .await;

    let client = new_hyper_client();
    let result = fetch_next(&client, &server.host()).await.unwrap();
    assert_eq!(result.deadline_ms, 9999);
    assert_eq!(result.trace_id.as_deref(), Some("trace-1"));
    assert_eq!(result.client_context.as_deref(), Some("ctx-1"));
    assert_eq!(result.cognito_identity.as_deref(), Some("cog-1"));
    assert_eq!(&result.body[..], b"payload");
}

#[tokio::test]
async fn next_missing_request_id() {
    let server = MockServer::start(next_ok(
        &[(H_FUNCTION_ARN, "arn")],
        "",
    ))
    .await;

    let client = new_hyper_client();
    let err = fetch_next(&client, &server.host()).await.unwrap_err();
    assert_eq!(err.to_string(), "Request ID absent");
}

#[tokio::test]
async fn next_missing_function_arn() {
    let server = MockServer::start(next_ok(
        &[(H_AWS_REQUEST_ID, "rid")],
        "",
    ))
    .await;

    let client = new_hyper_client();
    let err = fetch_next(&client, &server.host()).await.unwrap_err();
    assert_eq!(err.to_string(), "Function ARN absent");
}

#[tokio::test]
async fn next_empty_body() {
    let server = MockServer::start(next_ok(
        &[
            (H_AWS_REQUEST_ID, "rid"),
            (H_FUNCTION_ARN, "arn"),
        ],
        "",
    ))
    .await;

    let client = new_hyper_client();
    let result = fetch_next(&client, &server.host()).await.unwrap();
    assert!(result.body.is_empty());
}

// ── post_response tests ───────────────────────────────────────────

#[tokio::test]
async fn report_success_basic() {
    let server = MockServer::start(ok_empty()).await;

    let client = new_hyper_client();
    post_response(&client, &server.host(), "req-456", b"response body")
        .await
        .unwrap();

    let reqs = server.take_requests();
    assert_eq!(reqs.len(), 1);
    let (parts, body) = &reqs[0];
    assert_eq!(
        parts.uri.path(),
        "/2018-06-01/runtime/invocation/req-456/response"
    );
    assert_eq!(parts.method, http::Method::POST);
    assert_eq!(&body[..], b"response body");
    let ua = parts.headers.get("user-agent").unwrap().to_str().unwrap();
    assert!(ua.starts_with("com.smirnoal.crema-hyper/"));
}

#[tokio::test]
async fn report_success_empty_body() {
    let server = MockServer::start(ok_empty()).await;

    let client = new_hyper_client();
    post_response(&client, &server.host(), "req-789", b"")
        .await
        .unwrap();

    let reqs = server.take_requests();
    assert_eq!(reqs.len(), 1);
    let (_, body) = &reqs[0];
    assert!(body.is_empty());
}
