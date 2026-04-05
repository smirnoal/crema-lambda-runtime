package com.smirnoal.crema.rapid.client;

import com.smirnoal.crema.rapid.client.dto.ErrorRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JNI streaming tests (Linux only — native library).
 */
@EnabledOnOs(OS.LINUX)
class StreamingResponseRustTest extends MockServerBaseRust {

    @Test
    void setContentType_isSentOnWire() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(202));

        StreamingResponseHandle handle = runtimeClient.startStreamingResponse("req-ct");
        handle.responseStream().setContentType("text/event-stream");
        handle.responseStream().write("data".getBytes());
        handle.complete();

        RecordedRequest req = mockWebServer.takeRequest();
        assertEquals("text/event-stream", req.getHeader("Content-Type"));
    }

    @Test
    void startStreamingResponse_sendsChunkedWithStreamingHeaders() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(202));

        StreamingResponseHandle handle = runtimeClient.startStreamingResponse("req-456");
        handle.responseStream().write("chunk1".getBytes());
        handle.responseStream().write("chunk2".getBytes());
        handle.complete();

        RecordedRequest req = mockWebServer.takeRequest();
        assertEquals("/2018-06-01/runtime/invocation/req-456/response", req.getPath());
        assertEquals("POST", req.getMethod());
        assertEquals("streaming", req.getHeader("Lambda-Runtime-Function-Response-Mode"));
        String body = req.getBody().readUtf8();
        assertTrue(body.contains("chunk1"));
        assertTrue(body.contains("chunk2"));
    }

    /**
     * Uses {@link JniStreamingTestServer}: OkHttp MockWebServer cannot parse chunked request bodies
     * with trailer fields ({@code Lambda-Runtime-Function-Error-*}) and crashes the connection,
     * which blocks the client (see MockWebServer {@code readEmptyLine}).
     */
    @Test
    void startStreamingResponse_fail_completesWithoutThrowing() throws Exception {
        JniStreamingTestServer server = new JniStreamingTestServer();
        server.start();
        try {
            server.enqueue(JniStreamingTestServer.TestResponse.of(202));
            String hostPort = server.getHostName() + ":" + server.getPort();
            HyperLambdaRapidHttpClient client = new HyperLambdaRapidHttpClient(hostPort);

            StreamingResponseHandle handle = client.startStreamingResponse("req-789");
            handle.responseStream().write("partial".getBytes());
            handle.fail(new LambdaError(ErrorRequest.builder()
                    .withErrorType("java.lang.RuntimeException")
                    .withErrorMessage("mid-stream failure")
                    .withStackTrace(new String[]{"at Foo.bar(Foo.java:1)"})
                    .build()));

            assertInstanceOf(HyperResponseStream.class, handle.responseStream());

            JniStreamingTestServer.RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
            assertEquals("/2018-06-01/runtime/invocation/req-789/response", req.getPath());
            assertTrue(req.getBodyUtf8().contains("partial"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    void startStreamingResponse_emptyComplete() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(202));

        StreamingResponseHandle handle = runtimeClient.startStreamingResponse("req-empty");
        handle.complete();

        RecordedRequest req = mockWebServer.takeRequest();
        assertEquals("/2018-06-01/runtime/invocation/req-empty/response", req.getPath());
    }
}
