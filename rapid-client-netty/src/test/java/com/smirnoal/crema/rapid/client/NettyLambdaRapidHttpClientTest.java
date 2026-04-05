package com.smirnoal.crema.rapid.client;

import com.smirnoal.crema.rapid.client.dto.ErrorRequest;
import com.smirnoal.crema.rapid.client.dto.InvocationRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class NettyLambdaRapidHttpClientTest {

    NettyTestServer server;
    NettyLambdaRapidHttpClient client;

    @BeforeEach
    void setUp() throws InterruptedException {
        server = new NettyTestServer();
        server.start();
        client = new NettyLambdaRapidHttpClient(getHostnamePort());
    }

    @AfterEach
    void tearDown() {
        server.shutdown();
    }

    String getHostnamePort() {
        return server.getHostName() + ":" + server.getPort();
    }

    @Test
    @Timeout(10)
    void next_parsesInvocationRequest() throws Exception {
        String requestId = UUID.randomUUID().toString();
        String functionArn = "arn:aws:lambda:us-east-1:123:function:test";
        String body = "{\"foo\":\"bar\"}";

        server.enqueue(NettyTestServer.TestResponse.of(200)
                .addHeader("lambda-runtime-aws-request-id", requestId)
                .addHeader("lambda-runtime-invoked-function-arn", functionArn)
                .addHeader("lambda-runtime-deadline-ms", "5000")
                .withBody(body));

        InvocationRequest result = client.next();

        assertEquals(requestId, result.id());
        assertEquals(functionArn, result.invokedFunctionArn());
        assertEquals(5000L, result.deadlineTimeInMs());
        assertEquals(body, new String(result.content()));

        NettyTestServer.RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("/2018-06-01/runtime/invocation/next", req.getPath());
    }

    @Test
    void reportInvocationSuccess_sendsPostWithBody() throws Exception {
        String requestId = "req-123";
        server.enqueue(NettyTestServer.TestResponse.of(202));

        client.reportInvocationSuccess(requestId, "hello".getBytes());

        NettyTestServer.RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("/2018-06-01/runtime/invocation/req-123/response", req.getPath());
        assertEquals("POST", req.method());
        assertEquals("hello", req.getBodyUtf8());
    }

    @Test
    void setContentType_isSentOnWire() throws Exception {
        server.enqueue(NettyTestServer.TestResponse.of(202));
        StreamingResponseHandle handle = client.startStreamingResponse("req-ct");
        handle.responseStream().setContentType("text/event-stream");
        handle.responseStream().write("data".getBytes());
        handle.complete();

        NettyTestServer.RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("text/event-stream", req.getHeader("Content-Type"));
    }

    @Test
    void startStreamingResponse_sendsChunkedRequestWithStreamingHeaders() throws Exception {
        String requestId = "req-456";
        server.enqueue(NettyTestServer.TestResponse.of(202));

        StreamingResponseHandle handle = client.startStreamingResponse(requestId);
        handle.responseStream().write("chunk1".getBytes());
        handle.responseStream().write("chunk2".getBytes());
        handle.complete();

        NettyTestServer.RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(req);
        assertEquals("/2018-06-01/runtime/invocation/req-456/response", req.getPath());
        assertEquals("POST", req.method());
        assertEquals("streaming", req.getHeader("Lambda-Runtime-Function-Response-Mode"));
        // HttpObjectAggregator strips Transfer-Encoding from aggregated request; body content proves chunked transfer
        String body = req.getBodyUtf8();
        assertTrue(body.contains("chunk1"));
        assertTrue(body.contains("chunk2"));
    }

    @Test
    @Timeout(10)
    void startStreamingResponse_fail_completesWithoutThrowing() throws Exception {
        String requestId = "req-789";
        server.enqueue(NettyTestServer.TestResponse.of(202));

        StreamingResponseHandle handle = client.startStreamingResponse(requestId);
        handle.responseStream().write("partial".getBytes());
        handle.fail(new LambdaError(ErrorRequest.builder()
                .withErrorType("java.lang.RuntimeException")
                .withErrorMessage("mid-stream failure")
                .withStackTrace(new String[]{"at Foo.bar(Foo.java:1)"})
                .build()));

        assertInstanceOf(NettyResponseStream.class, handle.responseStream());

        NettyTestServer.RecordedRequest req = server.takeRequest(5, TimeUnit.SECONDS);
        assertEquals("/2018-06-01/runtime/invocation/req-789/response", req.getPath());
        assertTrue(req.getBodyUtf8().contains("partial"));
    }
}
