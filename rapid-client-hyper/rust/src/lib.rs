//! JNI bridge + hyper client for Lambda Runtime API `/next`, `/response`, and streaming `/response`.

#![allow(non_snake_case)]

mod client;
mod jni;
mod types;

use std::time::Duration;

const USER_AGENT: &str = concat!("com.smirnoal.crema-hyper/", env!("CARGO_PKG_VERSION"));
const PATH_INVOCATION_PREFIX: &str = "/2018-06-01/runtime/invocation/";

const H_AWS_REQUEST_ID: &str = "lambda-runtime-aws-request-id";
const H_FUNCTION_ARN: &str = "lambda-runtime-invoked-function-arn";
const H_DEADLINE_MS: &str = "lambda-runtime-deadline-ms";
const H_TRACE_ID: &str = "lambda-runtime-trace-id";
const H_CLIENT_CTX: &str = "lambda-runtime-client-context";
const H_COGNITO: &str = "lambda-runtime-cognito-identity";

const H_STREAMING_MODE: &str = "Lambda-Runtime-Function-Response-Mode";
const V_STREAMING: &str = "streaming";
const H_TRAILER_DECL: &str = "Trailer";
const H_TRAILER_DECL_VALUE: &str =
    "Lambda-Runtime-Function-Error-Type, Lambda-Runtime-Function-Error-Body";
const TRAILER_ERROR_TYPE: &str = "Lambda-Runtime-Function-Error-Type";
const TRAILER_ERROR_BODY: &str = "Lambda-Runtime-Function-Error-Body";

const REQUEST_TIMEOUT: Duration = Duration::from_secs(14 * 24 * 60 * 60);

const STREAMING_CHANNEL_CAPACITY: usize = 8;
