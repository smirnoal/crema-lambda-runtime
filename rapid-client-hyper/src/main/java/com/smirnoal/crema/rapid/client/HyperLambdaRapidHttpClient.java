package com.smirnoal.crema.rapid.client;

import com.smirnoal.crema.log.RicLog;
import com.smirnoal.crema.log.RicLog.RicLogger;
import com.smirnoal.crema.rapid.client.dto.InvocationRequest;

import java.util.Objects;

import static com.smirnoal.crema.rapid.client.RuntimeApiConstants.PATH_INVOCATION;

/**
 * Runtime API client: {@code /next}, buffered {@code .../response}, and streaming
 * {@code .../response} via Rust (hyper) + JNI; other calls lazy-delegate to
 * {@link JdkHttpClientProvider} so {@code java.net.http} is not loaded until error or SnapStart
 * paths run.
 */
public final class HyperLambdaRapidHttpClient implements LambdaRapidHttpClient {

    private static final RicLogger log = RicLog.getLogger("client");

    static {
        NativeLibraryLoader.load();
    }

    private final String runtimeApiHost;
    private volatile LambdaRapidHttpClient jdkDelegate;
    private final long nativeHandle;

    public HyperLambdaRapidHttpClient(String runtimeApiHost) {
        this.runtimeApiHost = Objects.requireNonNull(runtimeApiHost, "host cannot be null");
        log.message(() -> "connecting to " + runtimeApiHost);
        long h = nativeCreate(runtimeApiHost);
        if (h == 0) {
            throw new IllegalStateException("nativeCreate returned null handle");
        }
        this.nativeHandle = h;
        log.message("connected");
    }

    private LambdaRapidHttpClient delegate() {
        LambdaRapidHttpClient d = jdkDelegate;
        if (d == null) {
            synchronized (this) {
                d = jdkDelegate;
                if (d == null) {
                    jdkDelegate = d = new JdkHttpClientProvider().create(runtimeApiHost);
                }
            }
        }
        return d;
    }

    private static native long nativeCreate(String host);

    private static native void nativeDestroy(long handle);

    private native InvocationRequest nativeNext(long handle);

    private native void nativeReportInvocationSuccess(long handle, String requestId, byte[] body);

    static native long nativeStreamingStart(long handle, String requestId);

    static native void nativeStreamingSetContentType(long handle, long session, String contentType);

    static native void nativeStreamingWrite(long handle, long session, byte[] data, int off, int len);

    static native void nativeStreamingComplete(long handle, long session);

    static native void nativeStreamingFail(long handle, long session, String errorType, String errorBodyBase64);

    @Override
    public InvocationRequest next() {
        log.message(() -> "next() GET " + PATH_INVOCATION + "next");
        InvocationRequest req = nativeNext(nativeHandle);
        byte[] body = req.content();
        log.message(() -> "next() id=" + req.id() + " contentLen=" + (body != null ? body.length : 0));
        return req;
    }

    @Override
    public void reportInvocationSuccess(String requestId, byte[] response) {
        log.message(() -> "reportInvocationSuccess POST " + PATH_INVOCATION + requestId + "/response" + " bodyLen=" + (response != null ? response.length : 0));
        nativeReportInvocationSuccess(nativeHandle, requestId, response != null ? response : new byte[0]);
        log.message(() -> "reportInvocationSuccess completed requestId=" + requestId);
    }

    @Override
    public void initError(LambdaError error) {
        delegate().initError(error);
    }

    @Override
    public void reportInvocationError(String requestId, LambdaError error) {
        delegate().reportInvocationError(requestId, error);
    }

    @Override
    public void restoreNext() {
        delegate().restoreNext();
    }

    @Override
    public void reportRestoreError(LambdaError error) {
        delegate().reportRestoreError(error);
    }

    @Override
    public StreamingResponseHandle startStreamingResponse(String requestId) {
        Objects.requireNonNull(requestId, "requestId");
        RicLog.getLogger("streaming").message(() -> "startStreamingResponse POST " + PATH_INVOCATION + requestId + "/response" + " (chunked)");
        long session = nativeStreamingStart(nativeHandle, requestId);
        if (session == 0L) {
            // Normally unreachable: nativeStreamingStart throws a JNI exception before returning 0.
            throw new LambdaRapidClientException("nativeStreamingStart returned null session");
        }
        return new HyperStreamingResponseHandle(nativeHandle, session);
    }
}
