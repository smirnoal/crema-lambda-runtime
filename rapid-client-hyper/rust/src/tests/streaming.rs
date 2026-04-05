use std::convert::Infallible;
use std::net::SocketAddr;
use std::sync::{Arc, Mutex};

use bytes::{Bytes, BytesMut};
use http::header::HeaderMap;
use http::request::Parts;
use http_body_util::{BodyExt, Full};
use hyper::body::Incoming;
use hyper::server::conn::http1;
use hyper::service::service_fn;
use hyper::{Request, Response};
use hyper_util::rt::TokioIo;
use tokio::net::TcpListener;
use tokio::sync::oneshot;

use super::{ClientError, NativeClient};
use crate::{
    H_STREAMING_MODE, H_TRAILER_DECL, H_TRAILER_DECL_VALUE, TRAILER_ERROR_BODY,
    TRAILER_ERROR_TYPE, V_STREAMING,
};

struct StreamingRecordedRequest {
    parts: Parts,
    body: Bytes,
    trailers: Option<HeaderMap>,
}

struct StreamingMockServer {
    addr: SocketAddr,
    requests: Arc<Mutex<Vec<StreamingRecordedRequest>>>,
    _shutdown: oneshot::Sender<()>,
}

impl StreamingMockServer {
    async fn start() -> Self {
        Self::start_with_status(202).await
    }

    async fn start_with_status(status: u16) -> Self {
        let requests: Arc<Mutex<Vec<StreamingRecordedRequest>>> = Arc::default();
        let listener = TcpListener::bind("127.0.0.1:0").await.unwrap();
        let addr = listener.local_addr().unwrap();
        let (tx, mut rx) = oneshot::channel::<()>();
        let reqs = requests.clone();

        tokio::spawn(async move {
            loop {
                tokio::select! {
                    result = listener.accept() => {
                        let (stream, _) = result.unwrap();
                        let reqs = reqs.clone();
                        tokio::spawn(async move {
                            let io = TokioIo::new(stream);
                            let _ = http1::Builder::new()
                                .serve_connection(
                                    io,
                                    service_fn(move |req: Request<Incoming>| {
                                        let reqs = reqs.clone();
                                        async move {
                                            let (parts, mut body) = req.into_parts();
                                            let mut acc = BytesMut::new();
                                            let mut trailers: Option<HeaderMap> = None;
                                            while let Some(item) = body.frame().await {
                                                let frame = item.unwrap();
                                                if frame.is_trailers() {
                                                    if let Some(t) = frame.trailers_ref() {
                                                        trailers = Some(t.clone());
                                                    }
                                                } else if let Some(chunk) = frame.data_ref() {
                                                    acc.extend_from_slice(chunk);
                                                }
                                            }
                                            let body_bytes = acc.freeze();
                                            reqs.lock().unwrap().push(StreamingRecordedRequest {
                                                parts,
                                                body: body_bytes,
                                                trailers,
                                            });
                                            let resp = Response::builder()
                                                .status(status)
                                                .body(Full::new(Bytes::new()))
                                                .unwrap();
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

        StreamingMockServer {
            addr,
            requests,
            _shutdown: tx,
        }
    }

    fn host(&self) -> String {
        self.addr.to_string()
    }

    fn take_requests(&self) -> Vec<StreamingRecordedRequest> {
        std::mem::take(&mut self.requests.lock().unwrap())
    }
}

#[test]
fn streaming_basic_complete() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let server = rt.block_on(StreamingMockServer::start());
    let client = NativeClient::new(server.host()).unwrap();
    let mut sess = client.start_streaming("req-456").unwrap();
    client
        .streaming_write(&mut sess, Bytes::from("chunk1"))
        .unwrap();
    client
        .streaming_write(&mut sess, Bytes::from("chunk2"))
        .unwrap();
    client.streaming_complete(&mut sess).unwrap();

    let reqs = server.take_requests();
    assert_eq!(reqs.len(), 1);
    let r = &reqs[0];
    assert_eq!(
        r.parts.uri.path(),
        "/2018-06-01/runtime/invocation/req-456/response"
    );
    assert_eq!(r.parts.method, http::Method::POST);
    assert_eq!(
        r.parts.headers.get(H_STREAMING_MODE).unwrap().to_str().unwrap(),
        V_STREAMING
    );
    // §2.1: Content-Type, Trailer declaration, and Transfer-Encoding must be present
    assert_eq!(
        r.parts.headers.get("content-type").unwrap().to_str().unwrap(),
        "application/octet-stream"
    );
    assert_eq!(
        r.parts.headers.get(H_TRAILER_DECL).unwrap().to_str().unwrap(),
        H_TRAILER_DECL_VALUE
    );
    assert_eq!(
        r.parts.headers.get("transfer-encoding").unwrap().to_str().unwrap(),
        "chunked"
    );
    let body = String::from_utf8_lossy(&r.body);
    assert!(body.contains("chunk1"));
    assert!(body.contains("chunk2"));
    assert!(r.trailers.is_none());
}

#[test]
fn streaming_empty_complete() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let server = rt.block_on(StreamingMockServer::start());
    let client = NativeClient::new(server.host()).unwrap();
    let mut sess = client.start_streaming("req-empty").unwrap();
    client.streaming_complete(&mut sess).unwrap();

    let reqs = server.take_requests();
    assert_eq!(reqs.len(), 1);
    assert!(reqs[0].body.is_empty());
    assert!(reqs[0].trailers.is_none());
}

#[test]
fn streaming_fail_completes_after_partial_write() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let server = rt.block_on(StreamingMockServer::start());
    let client = NativeClient::new(server.host()).unwrap();
    let mut sess = client.start_streaming("req-789").unwrap();
    client
        .streaming_write(&mut sess, Bytes::from("partial"))
        .unwrap();
    client
        .streaming_fail(
            &mut sess,
            "java.lang.RuntimeException",
            "eyJlcnJvclR5cGUiOiJqYXZhLmxhbmcuUnVudGltZUV4Y2VwdGlvbiJ9",
        )
        .unwrap();

    let reqs = server.take_requests();
    assert_eq!(reqs.len(), 1);
    let r = &reqs[0];
    let body = String::from_utf8_lossy(&r.body);
    assert!(body.contains("partial"));
    // Hyper's HTTP/1.1 server (Incoming body) does not expose request body trailers via
    // frame() in the version used here. Wire-level verification is in
    // streaming_fail_sends_trailers_on_wire which uses a raw TCP server.
    if let Some(tr) = r.trailers.as_ref() {
        assert_eq!(
            tr.get(TRAILER_ERROR_TYPE).unwrap().to_str().unwrap(),
            "java.lang.RuntimeException"
        );
        assert!(!tr.get(TRAILER_ERROR_BODY).unwrap().is_empty());
    }
}

/// Verifies that the client actually encodes error trailers in the HTTP/1.1 wire format.
/// Uses a raw TCP server (no hyper) to read the raw chunked body and inspect trailers
/// that hyper's server-side Incoming body does not expose via frame().
#[test]
fn streaming_fail_sends_trailers_on_wire() {
    use std::io::{BufRead, BufReader, Write};
    use std::net::TcpListener;
    use std::sync::mpsc;

    let tcp_listener = TcpListener::bind("127.0.0.1:0").unwrap();
    let addr = tcp_listener.local_addr().unwrap();
    let (tx, rx) = mpsc::channel::<String>();

    std::thread::spawn(move || {
        let (stream, _) = tcp_listener.accept().unwrap();
        stream
            .set_read_timeout(Some(std::time::Duration::from_secs(5)))
            .unwrap();
        let mut reader = BufReader::new(stream.try_clone().unwrap());
        let mut writer = stream;
        let mut all = String::new();

        // Skip request headers (read until blank line)
        loop {
            let mut line = String::new();
            if reader.read_line(&mut line).unwrap_or(0) == 0 {
                break;
            }
            all.push_str(&line);
            if line == "\r\n" {
                break;
            }
        }

        // Read chunked body line by line
        loop {
            let mut size_line = String::new();
            if reader.read_line(&mut size_line).unwrap_or(0) == 0 {
                break;
            }
            all.push_str(&size_line);
            let hex = size_line.trim().split(';').next().unwrap_or("").trim();
            let chunk_size = usize::from_str_radix(hex, 16).unwrap_or(0);
            if chunk_size == 0 {
                // Read trailer headers until empty line
                loop {
                    let mut tline = String::new();
                    if reader.read_line(&mut tline).unwrap_or(0) == 0 {
                        break;
                    }
                    all.push_str(&tline);
                    if tline == "\r\n" {
                        break;
                    }
                }
                break;
            }
            let mut chunk_data = vec![0u8; chunk_size];
            std::io::Read::read_exact(&mut reader, &mut chunk_data).unwrap();
            all.push_str(&String::from_utf8_lossy(&chunk_data));
            let mut crlf = String::new();
            reader.read_line(&mut crlf).unwrap();
        }

        writer
            .write_all(b"HTTP/1.1 202 Accepted\r\nContent-Length: 0\r\n\r\n")
            .unwrap();
        let _ = tx.send(all);
    });

    let client = NativeClient::new(addr.to_string()).unwrap();
    let mut sess = client.start_streaming("req-trail-wire").unwrap();
    client
        .streaming_write(&mut sess, Bytes::from("partial"))
        .unwrap();
    client
        .streaming_fail(
            &mut sess,
            "java.lang.RuntimeException",
            "eyJlcnJvclR5cGUiOiJqYXZhLmxhbmcuUnVudGltZUV4Y2VwdGlvbiJ9",
        )
        .unwrap();

    let raw = rx
        .recv_timeout(std::time::Duration::from_secs(5))
        .expect("server thread did not respond");
    assert!(
        raw.contains("Lambda-Runtime-Function-Error-Type"),
        "error-type trailer not found on wire:\n{raw}"
    );
    assert!(
        raw.contains("java.lang.RuntimeException"),
        "error-type value not found on wire:\n{raw}"
    );
}

#[test]
fn streaming_multiple_writes() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let server = rt.block_on(StreamingMockServer::start());
    let client = NativeClient::new(server.host()).unwrap();
    let mut sess = client.start_streaming("req-multi").unwrap();
    for i in 0..5 {
        client
            .streaming_write(&mut sess, Bytes::from(format!("b{i}")))
            .unwrap();
    }
    client.streaming_complete(&mut sess).unwrap();

    let reqs = server.take_requests();
    let body = String::from_utf8_lossy(&reqs[0].body);
    for i in 0..5 {
        assert!(body.contains(&format!("b{i}")));
    }
}

#[test]
fn streaming_user_agent() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let server = rt.block_on(StreamingMockServer::start());
    let client = NativeClient::new(server.host()).unwrap();
    let mut sess = client.start_streaming("req-ua").unwrap();
    client.streaming_complete(&mut sess).unwrap();

    let reqs = server.take_requests();
    let ua = reqs[0]
        .parts
        .headers
        .get("user-agent")
        .unwrap()
        .to_str()
        .unwrap();
    assert!(ua.starts_with("com.smirnoal.crema-hyper/"));
    assert!(ua.contains("crema-hyper"));
}

#[test]
fn streaming_200_success() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let server = rt.block_on(StreamingMockServer::start_with_status(200));
    let client = NativeClient::new(server.host()).unwrap();
    let mut sess = client.start_streaming("req-200").unwrap();
    client.streaming_write(&mut sess, Bytes::from("data")).unwrap();
    // §2.3: 200 must be accepted as success, same as 202
    client.streaming_complete(&mut sess).unwrap();
}

#[test]
fn streaming_error_status_rejected() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let server = rt.block_on(StreamingMockServer::start_with_status(500));
    let client = NativeClient::new(server.host()).unwrap();
    let mut sess = client.start_streaming("req-500").unwrap();
    let err = client.streaming_complete(&mut sess).unwrap_err();
    assert!(
        matches!(err, ClientError::StreamingResponseFailed(500)),
        "expected StreamingResponseFailed(500), got {err:?}"
    );
}

#[test]
fn streaming_complete_idempotent_at_rust_level() {
    // At the JNI level, double-complete is guarded by HyperStreamingResponseHandle.finished.
    // At the Rust API level, the second call returns an error because session.response is None.
    let rt = tokio::runtime::Runtime::new().unwrap();
    let server = rt.block_on(StreamingMockServer::start());
    let client = NativeClient::new(server.host()).unwrap();
    let mut sess = client.start_streaming("req-idem").unwrap();
    client.streaming_complete(&mut sess).unwrap();
    let err = client.streaming_complete(&mut sess).unwrap_err();
    assert!(
        matches!(err, ClientError::Http(_)),
        "expected Http error on double-complete, got {err:?}"
    );
}

#[test]
fn streaming_custom_content_type() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let server = rt.block_on(StreamingMockServer::start());
    let client = NativeClient::new(server.host()).unwrap();
    let mut sess = client.start_streaming("req-ct").unwrap();
    client.set_content_type(&mut sess, "text/event-stream").unwrap();
    client.streaming_write(&mut sess, Bytes::from("data: hello\n")).unwrap();
    client.streaming_complete(&mut sess).unwrap();

    let reqs = server.take_requests();
    assert_eq!(reqs.len(), 1);
    assert_eq!(
        reqs[0].parts.headers.get("content-type").unwrap().to_str().unwrap(),
        "text/event-stream"
    );
}

#[test]
fn streaming_set_content_type_after_write_fails() {
    let rt = tokio::runtime::Runtime::new().unwrap();
    let server = rt.block_on(StreamingMockServer::start());
    let client = NativeClient::new(server.host()).unwrap();
    let mut sess = client.start_streaming("req-ct-late").unwrap();
    client.streaming_write(&mut sess, Bytes::from("data")).unwrap();
    let err = client.set_content_type(&mut sess, "text/plain").unwrap_err();
    assert!(
        matches!(err, ClientError::Http(_)),
        "expected Http error on late set_content_type, got {err:?}"
    );
    client.streaming_complete(&mut sess).unwrap();
}
