use std::convert::Infallible;
use std::fmt;
use std::pin::Pin;
use std::task::{Context, Poll};

use bytes::Bytes;
use http_body::Body;
use http_body_util::Full;
use hyper::body::Frame;
use hyper_util::client::legacy::connect::HttpConnector;
use hyper_util::client::legacy::Client;
use tokio::sync::mpsc;

pub(crate) type HyperClient = Client<HttpConnector, ClientBody>;

/// Request body for the legacy hyper client: buffered `Full` or streaming channel.
pub(crate) enum ClientBody {
    Buffered(Full<Bytes>),
    Streaming(ChannelBody),
}

pub(crate) struct ChannelBody {
    rx: mpsc::Receiver<Frame<Bytes>>,
}

impl ChannelBody {
    pub(crate) fn new(rx: mpsc::Receiver<Frame<Bytes>>) -> Self {
        ChannelBody { rx }
    }
}

impl Body for ChannelBody {
    type Data = Bytes;
    type Error = Infallible;

    fn poll_frame(
        self: Pin<&mut Self>,
        cx: &mut Context<'_>,
    ) -> Poll<Option<Result<Frame<Self::Data>, Self::Error>>> {
        match self.get_mut().rx.poll_recv(cx) {
            Poll::Ready(Some(frame)) => Poll::Ready(Some(Ok(frame))),
            Poll::Ready(None) => Poll::Ready(None),
            Poll::Pending => Poll::Pending,
        }
    }
}

impl Body for ClientBody {
    type Data = Bytes;
    type Error = Infallible;

    fn poll_frame(
        self: Pin<&mut Self>,
        cx: &mut Context<'_>,
    ) -> Poll<Option<Result<Frame<Self::Data>, Self::Error>>> {
        match self.get_mut() {
            ClientBody::Buffered(f) => Pin::new(f).poll_frame(cx),
            ClientBody::Streaming(s) => Pin::new(s).poll_frame(cx),
        }
    }

    fn size_hint(&self) -> http_body::SizeHint {
        match self {
            ClientBody::Buffered(f) => f.size_hint(),
            ClientBody::Streaming(_) => http_body::SizeHint::default(),
        }
    }
}

#[derive(Debug)]
pub(crate) struct RawInvocationRequest {
    pub(crate) id: String,
    pub(crate) arn: String,
    pub(crate) deadline_ms: i64,
    pub(crate) trace_id: Option<String>,
    pub(crate) client_context: Option<String>,
    pub(crate) cognito_identity: Option<String>,
    pub(crate) body: Bytes,
}

#[derive(Debug)]
pub(crate) enum ClientError {
    Http(String),
    Timeout,
    MissingHeader(&'static str),
    /// Runtime API rejected the streaming POST (non-200/202).
    StreamingResponseFailed(u16),
}

impl fmt::Display for ClientError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ClientError::Http(msg) => write!(f, "{msg}"),
            ClientError::Timeout => write!(f, "request timed out"),
            ClientError::MissingHeader(name) => write!(f, "{name} absent"),
            ClientError::StreamingResponseFailed(code) => {
                write!(f, "Streaming response failed: {code}")
            }
        }
    }
}
