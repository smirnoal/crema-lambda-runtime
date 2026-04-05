use bytes::Bytes;
use jni::objects::{JByteArray, JClass, JObject, JString, JValue};
use jni::sys::{jint, jlong, jobject};
use jni::JNIEnv;

use crate::client::{NativeClient, StreamingSession};
use crate::types::{ClientError, RawInvocationRequest};

// ─── JNI helpers ──────────────────────────────────────────────────

fn throw_rapid(env: &mut JNIEnv, msg: &str) {
    let _ = env.throw_new(
        "com/smirnoal/crema/rapid/client/LambdaRapidClientException",
        msg,
    );
}

fn throw_io(env: &mut JNIEnv, msg: &str) {
    let _ = env.throw_new("java/io/IOException", msg);
}

fn throw_streaming_failed(env: &mut JNIEnv, code: u16) {
    // Match message shape of LambdaRapidClientException(String, int) constructor.
    let msg = format!("Streaming response failed: {code} Response code: '{code}'.");
    throw_rapid(env, &msg);
}

fn map_streaming_err(env: &mut JNIEnv, e: ClientError) {
    match e {
        ClientError::StreamingResponseFailed(c) => throw_streaming_failed(env, c),
        ClientError::Http(s) => throw_io(env, &s),
        ClientError::Timeout => throw_io(env, "request timed out"),
        ClientError::MissingHeader(n) => throw_rapid(env, &format!("{n} absent")),
    }
}

fn optional_jstring<'a>(
    env: &mut JNIEnv<'a>,
    value: Option<&str>,
) -> Result<JObject<'a>, jni::errors::Error> {
    match value {
        Some(s) => Ok(JObject::from(env.new_string(s)?)),
        None => Ok(JObject::null()),
    }
}

fn marshal_invocation_request(
    env: &mut JNIEnv,
    req: RawInvocationRequest,
) -> Result<jobject, jni::errors::Error> {
    let j_id = env.new_string(&req.id)?;
    let j_arn = env.new_string(&req.arn)?;
    let j_trace = optional_jstring(env, req.trace_id.as_deref())?;
    let j_client_ctx = optional_jstring(env, req.client_context.as_deref())?;
    let j_cognito = optional_jstring(env, req.cognito_identity.as_deref())?;

    let len = req.body.len();
    let jbytes = env.new_byte_array(len as i32)?;
    if len > 0 {
        // SAFETY: i8 and u8 have identical memory layout; JNI uses jbyte (i8).
        let body_i8 = unsafe { &*(req.body.as_ref() as *const [u8] as *const [i8]) };
        env.set_byte_array_region(&jbytes, 0, body_i8)?;
    }

    // Record canonical constructor: InvocationRequest(id, content, clientContext,
    //     cognitoIdentity, deadlineTimeInMs, invokedFunctionArn, xrayTraceId)
    let result = env.new_object(
        "com/smirnoal/crema/rapid/client/dto/InvocationRequest",
        "(Ljava/lang/String;[BLjava/lang/String;Ljava/lang/String;JLjava/lang/String;Ljava/lang/String;)V",
        &[
            JValue::Object(&j_id),
            JValue::Object(&jbytes),
            JValue::Object(&j_client_ctx),
            JValue::Object(&j_cognito),
            JValue::Long(req.deadline_ms),
            JValue::Object(&j_arn),
            JValue::Object(&j_trace),
        ],
    )?;

    Ok(result.into_raw())
}

// ─── JNI entry points ─────────────────────────────────────────────

#[no_mangle]
pub unsafe extern "system" fn Java_com_smirnoal_crema_rapid_client_HyperLambdaRapidHttpClient_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    host: JString,
) -> jlong {
    let host: String = match env.get_string(&host) {
        Ok(s) => s.into(),
        Err(e) => {
            throw_rapid(&mut env, &format!("invalid host: {e}"));
            return 0;
        }
    };

    match NativeClient::new(host) {
        Ok(c) => Box::into_raw(Box::new(c)) as jlong,
        Err(e) => {
            throw_rapid(&mut env, &format!("native client init failed: {e}"));
            0
        }
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_smirnoal_crema_rapid_client_HyperLambdaRapidHttpClient_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }
    drop(Box::from_raw(handle as *mut NativeClient));
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_smirnoal_crema_rapid_client_HyperLambdaRapidHttpClient_nativeNext(
    mut env: JNIEnv,
    _this: JObject,
    handle: jlong,
) -> jobject {
    if handle == 0 {
        throw_rapid(&mut env, "native handle is null");
        return std::ptr::null_mut();
    }
    let state = &*(handle as *const NativeClient);

    match state.next() {
        Ok(req) => match marshal_invocation_request(&mut env, req) {
            Ok(obj) => obj,
            Err(e) => {
                throw_rapid(&mut env, &format!("JNI error: {e}"));
                std::ptr::null_mut()
            }
        },
        Err(ClientError::MissingHeader(name)) => {
            throw_rapid(&mut env, &format!("{name} absent"));
            std::ptr::null_mut()
        }
        Err(e) => {
            throw_rapid(&mut env, &format!("Failed to get next invoke: {e}"));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_smirnoal_crema_rapid_client_HyperLambdaRapidHttpClient_nativeReportInvocationSuccess(
    mut env: JNIEnv,
    _this: JObject,
    handle: jlong,
    request_id: JString,
    body: JByteArray,
) {
    if handle == 0 {
        throw_rapid(&mut env, "native handle is null");
        return;
    }
    let state = &*(handle as *const NativeClient);

    let request_id: String = match env.get_string(&request_id) {
        Ok(s) => s.into(),
        Err(e) => {
            throw_rapid(&mut env, &format!("request id: {e}"));
            return;
        }
    };

    let body_len = match env.get_array_length(&body) {
        Ok(n) => n,
        Err(e) => {
            throw_rapid(&mut env, &format!("body: {e}"));
            return;
        }
    };
    let mut buf = vec![0u8; body_len as usize];
    if body_len > 0 {
        // SAFETY: i8 and u8 have identical memory layout; JNI uses jbyte (i8).
        let buf_i8 = unsafe { &mut *(buf.as_mut_slice() as *mut [u8] as *mut [i8]) };
        if let Err(e) = env.get_byte_array_region(&body, 0, buf_i8) {
            throw_rapid(&mut env, &format!("body read: {e}"));
            return;
        }
    }

    if let Err(e) = state.report_success(&request_id, &buf) {
        throw_rapid(&mut env, &format!("Failed to post invocation result: {e}"));
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_smirnoal_crema_rapid_client_HyperLambdaRapidHttpClient_nativeStreamingStart(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    request_id: JString,
) -> jlong {
    if handle == 0 {
        throw_rapid(&mut env, "native handle is null");
        return 0;
    }
    let state = &*(handle as *const NativeClient);
    let request_id: String = match env.get_string(&request_id) {
        Ok(s) => s.into(),
        Err(e) => {
            throw_rapid(&mut env, &format!("request id: {e}"));
            return 0;
        }
    };
    match state.start_streaming(&request_id) {
        Ok(session) => Box::into_raw(Box::new(session)) as jlong,
        Err(e) => {
            map_streaming_err(&mut env, e);
            0
        }
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_smirnoal_crema_rapid_client_HyperLambdaRapidHttpClient_nativeStreamingSetContentType(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    session_handle: jlong,
    content_type: JString,
) {
    if handle == 0 {
        throw_rapid(&mut env, "native client handle is null");
        return;
    }
    if session_handle == 0 {
        throw_rapid(&mut env, "streaming session handle is null");
        return;
    }
    let state = &*(handle as *const NativeClient);
    let session = &mut *(session_handle as *mut StreamingSession);
    let ct: String = match env.get_string(&content_type) {
        Ok(s) => s.into(),
        Err(e) => {
            throw_io(&mut env, &format!("content type: {e}"));
            return;
        }
    };
    if let Err(e) = state.set_content_type(session, &ct) {
        map_streaming_err(&mut env, e);
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_smirnoal_crema_rapid_client_HyperLambdaRapidHttpClient_nativeStreamingWrite(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    session_handle: jlong,
    data: JByteArray,
    off: jint,
    len: jint,
) {
    if handle == 0 {
        throw_rapid(&mut env, "native client handle is null");
        return;
    }
    if session_handle == 0 {
        throw_rapid(&mut env, "streaming session handle is null");
        return;
    }
    let state = &*(handle as *const NativeClient);
    let session = &mut *(session_handle as *mut StreamingSession);
    let body_len = match env.get_array_length(&data) {
        Ok(n) => n,
        Err(e) => {
            throw_io(&mut env, &format!("streaming write array: {e}"));
            return;
        }
    };
    if off < 0 || len < 0 {
        throw_io(&mut env, "streaming write: negative offset or length");
        return;
    }
    let off_u = off as usize;
    let len_u = len as usize;
    if off_u.saturating_add(len_u) > body_len as usize {
        throw_io(&mut env, "streaming write: range out of bounds");
        return;
    }
    let mut buf = vec![0u8; len_u];
    if len_u > 0 {
        let buf_i8 = unsafe { &mut *(buf.as_mut_slice() as *mut [u8] as *mut [i8]) };
        if let Err(e) = env.get_byte_array_region(&data, off, buf_i8) {
            throw_io(&mut env, &format!("streaming write read: {e}"));
            return;
        }
    }
    if let Err(e) = state.streaming_write(session, Bytes::from(buf)) {
        map_streaming_err(&mut env, e);
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_smirnoal_crema_rapid_client_HyperLambdaRapidHttpClient_nativeStreamingComplete(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    session_handle: jlong,
) {
    if handle == 0 {
        throw_rapid(&mut env, "native client handle is null");
        return;
    }
    if session_handle == 0 {
        throw_rapid(&mut env, "streaming session handle is null");
        return;
    }
    let state = &*(handle as *const NativeClient);
    let mut session = *Box::from_raw(session_handle as *mut StreamingSession);
    match state.streaming_complete(&mut session) {
        Ok(()) => {}
        Err(e) => map_streaming_err(&mut env, e),
    }
}

#[no_mangle]
pub unsafe extern "system" fn Java_com_smirnoal_crema_rapid_client_HyperLambdaRapidHttpClient_nativeStreamingFail(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    session_handle: jlong,
    error_type: JString,
    error_body_base64: JString,
) {
    if handle == 0 {
        throw_rapid(&mut env, "native client handle is null");
        return;
    }
    if session_handle == 0 {
        throw_rapid(&mut env, "streaming session handle is null");
        return;
    }
    // Consume the session unconditionally so it is freed on all exit paths, including
    // string-extraction failures below. Java has already set finished=true before calling here,
    // so the session cannot be accessed again regardless of outcome.
    //
    // Edge case: if nativeStreamingWrite was called first (session.started = true) and string
    // extraction fails here, dropping `session` also drops `session.sender`. The closed channel
    // signals end-of-body to the spawned HTTP task, which completes the POST without error
    // trailers. The runtime API receives what looks like a successful response; the failure is
    // only visible to the caller as a Java IOException. In practice env.get_string() cannot fail
    // for a Java-computed Base64 string, so this path is unreachable in production.
    let state = &*(handle as *const NativeClient);
    let mut session = *Box::from_raw(session_handle as *mut StreamingSession);
    let et: String = match env.get_string(&error_type) {
        Ok(s) => s.into(),
        Err(e) => {
            throw_io(&mut env, &format!("error type: {e}"));
            return;
        }
    };
    let eb: String = match env.get_string(&error_body_base64) {
        Ok(s) => s.into(),
        Err(e) => {
            throw_io(&mut env, &format!("error body: {e}"));
            return;
        }
    };
    match state.streaming_fail(&mut session, &et, &eb) {
        Ok(()) => {}
        Err(e) => map_streaming_err(&mut env, e),
    }
}
