package com.smirnoal.crema.rapid.client;

import com.smirnoal.crema.log.RicLog;
import com.smirnoal.crema.log.RicLog.RicLogger;
import com.smirnoal.crema.rapid.client.dto.ErrorRequest;
import com.smirnoal.crema.rapid.client.serde.JsonSerializer;
import com.smirnoal.crema.stream.ResponseStream;

import java.io.IOException;
import java.util.Base64;

/**
 * JNI streaming handle: chunked POST with optional error trailers (parity with Netty client).
 */
final class HyperStreamingResponseHandle implements StreamingResponseHandle {

    private static final RicLogger log = RicLog.getLogger("streaming");

    private final long nativeHandle;
    private final long sessionHandle;
    private final HyperResponseStream responseStream;
    // Lifecycle guard: the runtime calls either complete() or fail(), never both, on the single
    // invocation-loop thread. The handler only receives responseStream(), so it cannot hold a
    // reference to this handle — cross-thread access is structurally impossible.
    private boolean finished;

    HyperStreamingResponseHandle(long nativeHandle, long sessionHandle) {
        this.nativeHandle = nativeHandle;
        this.sessionHandle = sessionHandle;
        this.responseStream = new HyperResponseStream(nativeHandle, sessionHandle);
    }

    @Override
    public ResponseStream responseStream() {
        return responseStream;
    }

    @Override
    public void complete() throws IOException {
        if (finished) {
            return;
        }
        finished = true;
        log.message("complete() sending empty last content");
        HyperLambdaRapidHttpClient.nativeStreamingComplete(nativeHandle, sessionHandle);
    }

    @Override
    public void fail(LambdaError error) throws IOException {
        if (finished) {
            return;
        }
        finished = true;

        ErrorRequest req = error.errorRequest();
        log.message(() ->
                "fail() sending error trailers errorType="
                        + (req.errorType() != null ? req.errorType() : "java.lang.Throwable"));
        String errorType = req.errorType() != null ? req.errorType() : "java.lang.Throwable";
        byte[] errorBodyJson = JsonSerializer.serialize(req);
        String errorBodyBase64 = Base64.getEncoder().encodeToString(errorBodyJson);

        HyperLambdaRapidHttpClient.nativeStreamingFail(
                nativeHandle, sessionHandle, errorType, errorBodyBase64);
    }
}
