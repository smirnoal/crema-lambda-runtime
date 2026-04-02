use std::str::FromStr;

use bytes::Bytes;
use http::header::{self, HeaderMap, HeaderName, HeaderValue};
use http_body_util::{BodyExt, Full};
use hyper::body::Frame;
use hyper::{Request, Uri};
use hyper_util::client::legacy::connect::HttpConnector;
use hyper_util::client::legacy::Client;
use hyper_util::rt::TokioExecutor;
use tokio::runtime::Runtime;
use tokio::sync::mpsc;
use tokio::task::JoinHandle;

use crate::types::{ChannelBody, ClientBody, ClientError, HyperClient, RawInvocationRequest};
use crate::{
    H_AWS_REQUEST_ID, H_CLIENT_CTX, H_COGNITO, H_DEADLINE_MS, H_FUNCTION_ARN, H_STREAMING_MODE,
    H_TRAILER_DECL, H_TRAILER_DECL_VALUE, H_TRACE_ID, PATH_INVOCATION_PREFIX, REQUEST_TIMEOUT,
    STREAMING_CHANNEL_CAPACITY, TRAILER_ERROR_BODY, TRAILER_ERROR_TYPE, USER_AGENT, V_STREAMING,
};

// ─── Streaming session (native pointer behind jlong) ─────────────

pub(crate) struct StreamingSession {
    sender: Option<mpsc::Sender<Frame<Bytes>>>,
    receiver: Option<mpsc::Receiver<Frame<Bytes>>>,
    response: Option<JoinHandle<Result<u16, String>>>,
    request_id: String,
    content_type: String,
    started: bool,
}

impl StreamingSession {
    fn new(
        sender: mpsc::Sender<Frame<Bytes>>,
        receiver: mpsc::Receiver<Frame<Bytes>>,
        request_id: String,
    ) -> Self {
        StreamingSession {
            sender: Some(sender),
            receiver: Some(receiver),
            response: None,
            request_id,
            content_type: "application/octet-stream".to_owned(),
            started: false,
        }
    }
}

// ─── NativeClient ─────────────────────────────────────────────────

pub(crate) struct NativeClient {
    runtime: Runtime,
    host: String,
    client: HyperClient,
}

impl NativeClient {
    pub(crate) fn new(host: String) -> Result<Self, String> {
        let runtime = tokio::runtime::Builder::new_current_thread()
            .enable_all()
            .build()
            .map_err(|e| e.to_string())?;
        let client = {
            let _guard = runtime.enter();
            new_hyper_client()
        };
        Ok(NativeClient { runtime, host, client })
    }

    fn block_on<R>(&self, fut: impl std::future::Future<Output = R>) -> R {
        self.runtime.block_on(fut)
    }

    pub(crate) fn next(&self) -> Result<RawInvocationRequest, ClientError> {
        self.block_on(fetch_next(&self.client, &self.host))
    }

    pub(crate) fn report_success(
        &self,
        request_id: &str,
        body: &[u8],
    ) -> Result<(), ClientError> {
        self.block_on(post_response(&self.client, &self.host, request_id, body))
    }

    pub(crate) fn start_streaming(
        &self,
        request_id: &str,
    ) -> Result<StreamingSession, ClientError> {
        let (tx, rx) = mpsc::channel::<Frame<Bytes>>(STREAMING_CHANNEL_CAPACITY);
        Ok(StreamingSession::new(tx, rx, request_id.to_owned()))
    }

    /// `&self` is not used by the implementation but is kept for API uniformity with the other
    /// streaming methods, so the JNI layer can validate the client handle before touching the
    /// session pointer.
    pub(crate) fn set_content_type(
        &self,
        session: &mut StreamingSession,
        content_type: &str,
    ) -> Result<(), ClientError> {
        if session.started {
            return Err(ClientError::Http(
                "cannot set content-type after first write".into(),
            ));
        }
        session.content_type = content_type.to_owned();
        Ok(())
    }

    /// Lazily starts the HTTP POST on the first write, complete, or fail call.
    fn ensure_started(&self, session: &mut StreamingSession) -> Result<(), ClientError> {
        if session.started {
            return Ok(());
        }
        session.started = true;

        let rx = session
            .receiver
            .take()
            .ok_or_else(|| ClientError::Http("streaming receiver already consumed".into()))?;
        let body = ClientBody::Streaming(ChannelBody::new(rx));

        let uri: Uri = format!(
            "http://{}{}{}/response",
            self.host, PATH_INVOCATION_PREFIX, session.request_id
        )
        .parse()
        .map_err(|e| ClientError::Http(format!("invalid URI: {e}")))?;

        let trailer_decl = HeaderValue::from_str(H_TRAILER_DECL_VALUE)
            .map_err(|e| ClientError::Http(format!("trailer header value: {e}")))?;
        let ct = HeaderValue::from_str(&session.content_type)
            .map_err(|e| ClientError::Http(format!("content-type value: {e}")))?;
        let req = Request::post(uri)
            .header(header::USER_AGENT, USER_AGENT)
            .header(H_STREAMING_MODE, V_STREAMING)
            .header(header::CONTENT_TYPE, ct)
            .header(H_TRAILER_DECL, trailer_decl)
            .body(body)
            .map_err(|e| ClientError::Http(format!("request build failed: {e}")))?;

        let client = self.client.clone();
        let _guard = self.runtime.enter();
        let join = tokio::spawn(async move {
            let resp = client
                .request(req)
                .await
                .map_err(|e| e.to_string())?;
            let status = resp.status().as_u16();
            resp.into_body()
                .collect()
                .await
                .map_err(|e| e.to_string())?;
            Ok(status)
        });

        session.response = Some(join);
        Ok(())
    }

    pub(crate) fn streaming_write(
        &self,
        session: &mut StreamingSession,
        data: Bytes,
    ) -> Result<(), ClientError> {
        self.ensure_started(session)?;
        let sender = session
            .sender
            .as_ref()
            .ok_or_else(|| ClientError::Http("streaming session closed".into()))?;
        // block_on drives the current_thread runtime's spawned HTTP task while waiting for
        // channel backpressure to clear. This must be called from a plain OS thread, never
        // from within a tokio task on this runtime — block_on panics if re-entered.
        self.block_on(async {
            sender
                .send(Frame::data(data))
                .await
                .map_err(|_| ClientError::Http("streaming write failed".into()))
        })
    }

    pub(crate) fn streaming_complete(
        &self,
        session: &mut StreamingSession,
    ) -> Result<(), ClientError> {
        self.ensure_started(session)?;
        session.sender.take();
        let join = session
            .response
            .take()
            .ok_or_else(|| ClientError::Http("streaming session already finished".into()))?;
        let status = self.block_on(async {
            match join.await {
                Ok(Ok(s)) => Ok(s),
                Ok(Err(e)) => Err(ClientError::Http(e)),
                Err(e) => Err(ClientError::Http(format!("streaming task join: {e}"))),
            }
        })?;
        validate_streaming_response_status(status)
    }

    pub(crate) fn streaming_fail(
        &self,
        session: &mut StreamingSession,
        error_type: &str,
        error_body_base64: &str,
    ) -> Result<(), ClientError> {
        self.ensure_started(session)?;
        let sender = session
            .sender
            .as_ref()
            .ok_or_else(|| ClientError::Http("streaming session closed".into()))?;
        let mut trailers = HeaderMap::new();
        trailers.insert(
            HeaderName::from_bytes(TRAILER_ERROR_TYPE.as_bytes())
                .map_err(|e| ClientError::Http(format!("trailer name: {e}")))?,
            HeaderValue::from_str(error_type)
                .map_err(|e| ClientError::Http(format!("trailer type value: {e}")))?,
        );
        trailers.insert(
            HeaderName::from_bytes(TRAILER_ERROR_BODY.as_bytes())
                .map_err(|e| ClientError::Http(format!("trailer name: {e}")))?,
            HeaderValue::from_str(error_body_base64)
                .map_err(|e| ClientError::Http(format!("trailer body value: {e}")))?,
        );
        self.block_on(async {
            sender
                .send(Frame::trailers(trailers))
                .await
                .map_err(|_| ClientError::Http("streaming fail send trailers".into()))
        })?;
        session.sender.take();
        let join = session
            .response
            .take()
            .ok_or_else(|| ClientError::Http("streaming session already finished".into()))?;
        let status = self.block_on(async {
            match join.await {
                Ok(Ok(s)) => Ok(s),
                Ok(Err(e)) => Err(ClientError::Http(e)),
                Err(e) => Err(ClientError::Http(format!("streaming task join: {e}"))),
            }
        })?;
        validate_streaming_response_status(status)
    }
}

// ─── Async HTTP logic ─────────────────────────────────────────────

fn validate_streaming_response_status(status: u16) -> Result<(), ClientError> {
    if status == 200 || status == 202 {
        Ok(())
    } else {
        Err(ClientError::StreamingResponseFailed(status))
    }
}

fn new_hyper_client() -> HyperClient {
    let mut connector = HttpConnector::new();
    connector.set_connect_timeout(Some(REQUEST_TIMEOUT));
    connector.enforce_http(true);
    Client::builder(TokioExecutor::new())
        .pool_max_idle_per_host(1)
        .build(connector)
}

fn header_first<'a>(headers: &'a HeaderMap, name: &'static str) -> Option<&'a str> {
    headers.get(name).and_then(|v| v.to_str().ok())
}

async fn fetch_next(
    client: &HyperClient,
    host: &str,
) -> Result<RawInvocationRequest, ClientError> {
    let uri: Uri = format!("http://{}{}next", host, PATH_INVOCATION_PREFIX)
        .parse()
        .map_err(|e| ClientError::Http(format!("invalid URI: {e}")))?;

    let req = Request::get(uri)
        .header(header::USER_AGENT, USER_AGENT)
        .body(ClientBody::Buffered(Full::new(Bytes::new())))
        .map_err(|e| ClientError::Http(format!("request build failed: {e}")))?;

    let resp = tokio::time::timeout(REQUEST_TIMEOUT, client.request(req))
        .await
        .map_err(|_| ClientError::Timeout)?
        .map_err(|e| ClientError::Http(e.to_string()))?;

    let (parts, body) = resp.into_parts();
    let headers = parts.headers;

    let body = body
        .collect()
        .await
        .map_err(|e| ClientError::Http(e.to_string()))?
        .to_bytes();

    let id = header_first(&headers, H_AWS_REQUEST_ID)
        .ok_or(ClientError::MissingHeader("Request ID"))?
        .to_owned();
    let arn = header_first(&headers, H_FUNCTION_ARN)
        .ok_or(ClientError::MissingHeader("Function ARN"))?
        .to_owned();
    let deadline_ms = header_first(&headers, H_DEADLINE_MS)
        .and_then(|s| i64::from_str(s).ok())
        .unwrap_or(0);
    let trace_id = header_first(&headers, H_TRACE_ID).map(str::to_owned);
    let client_context = header_first(&headers, H_CLIENT_CTX).map(str::to_owned);
    let cognito_identity = header_first(&headers, H_COGNITO).map(str::to_owned);

    Ok(RawInvocationRequest {
        id,
        arn,
        deadline_ms,
        trace_id,
        client_context,
        cognito_identity,
        body,
    })
}

async fn post_response(
    client: &HyperClient,
    host: &str,
    request_id: &str,
    body: &[u8],
) -> Result<(), ClientError> {
    let uri: Uri = format!(
        "http://{}{}{}/response",
        host, PATH_INVOCATION_PREFIX, request_id
    )
    .parse()
    .map_err(|e| ClientError::Http(format!("invalid URI: {e}")))?;

    let req = Request::post(uri)
        .header(header::USER_AGENT, USER_AGENT)
        .body(ClientBody::Buffered(Full::new(Bytes::copy_from_slice(body))))
        .map_err(|e| ClientError::Http(format!("request build failed: {e}")))?;

    let resp = tokio::time::timeout(REQUEST_TIMEOUT, client.request(req))
        .await
        .map_err(|_| ClientError::Timeout)?
        .map_err(|e| ClientError::Http(e.to_string()))?;

    resp.into_body()
        .collect()
        .await
        .map_err(|e| ClientError::Http(e.to_string()))?;

    Ok(())
}

// ─── Tests ────────────────────────────────────────────────────────

#[cfg(test)]
#[path = "tests/buffered.rs"]
mod buffered_tests;

#[cfg(test)]
#[path = "tests/streaming.rs"]
mod streaming_tests;
