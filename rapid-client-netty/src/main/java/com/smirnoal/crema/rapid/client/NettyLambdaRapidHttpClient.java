package com.smirnoal.crema.rapid.client;

import com.smirnoal.crema.log.RicLog;
import com.smirnoal.crema.log.RicLog.RicLogger;
import com.smirnoal.crema.rapid.client.dto.InvocationRequest;
import com.smirnoal.crema.rapid.client.serde.JsonSerializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.smirnoal.crema.rapid.client.RuntimeApiConstants.*;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;

final class NettyLambdaRapidHttpClient implements LambdaRapidHttpClient {

    private static final RicLogger log = RicLog.getLogger("client");

    static final String USER_AGENT =
            "com.smirnoal.crema-netty/%s".formatted(System.getProperty("java.vendor.version"));

    private static final int REQUEST_TIMEOUT_SECONDS = (int) REQUEST_TIMEOUT.toSeconds();

    private final String runtimeApiHost;
    private final String host;
    private final int port;
    private final String invocationPathPrefix;
    private final Bootstrap bootstrap;

    private volatile Channel channel;
    private final ResponseCaptureHandler responseHandler = new ResponseCaptureHandler();

    NettyLambdaRapidHttpClient(String runtimeApiHost) {
        Objects.requireNonNull(runtimeApiHost, "host cannot be null");
        // runtimeApiHost is always host:port, both parts present
        this.runtimeApiHost = runtimeApiHost;
        int colon = runtimeApiHost.lastIndexOf(':');
        this.host = runtimeApiHost.substring(0, colon);
        this.port = Integer.parseInt(runtimeApiHost.substring(colon + 1));
        this.invocationPathPrefix = PATH_INVOCATION;

        EventLoopGroup eventLoopGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
        this.bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new HttpClientCodec())
                                .addLast(new HttpObjectAggregator(MAX_SYNC_RESPONSE_BYTES))
                                .addLast(responseHandler);
                    }
                });
    }

    private Channel getChannel() {
        if (channel == null || !channel.isActive()) {
            try {
                log.message(() -> "connecting to " + runtimeApiHost);
                ChannelFuture future = bootstrap.connect(host, port);
                channel = future.sync().channel();
                log.message("connected");
            } catch (Exception e) {
                throw new LambdaRapidClientException("Failed to connect to " + runtimeApiHost, e);
            }
        }
        return channel;
    }

    private FullHttpResponse execute(HttpRequest request) {
        request.headers()
                .set(HttpHeaderNames.HOST, runtimeApiHost)
                .set(HttpHeaderNames.USER_AGENT, USER_AGENT);
        CompletableFuture<FullHttpResponse> future = new CompletableFuture<>();
        responseHandler.setPendingFuture(future);

        Channel ch = getChannel();
        ch.eventLoop().execute(() -> ch.writeAndFlush(request));

        try {
            return future.get(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LambdaRapidClientException("Interrupted", e);
        } catch (ExecutionException e) {
            throw new LambdaRapidClientException("Request failed", e.getCause());
        } catch (TimeoutException e) {
            throw new LambdaRapidClientException("Request timed out", e);
        }
    }

    @Override
    public void initError(LambdaError error) {
        reportLambdaError(PATH_INIT_ERROR, error);
    }

    @Override
    public InvocationRequest next() {
        String path = invocationPathPrefix + "next";
        log.message(() -> "next() GET " + path);
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);

        FullHttpResponse response = execute(request);
        log.message(() -> "next() response status=" + response.status().code() + " contentLen=" + response.content().readableBytes());
        try {
            if (response.status().code() != HTTP_OK) {
                throw new LambdaRapidClientException(path, response.status().code());
            }
            HttpHeaders headers = response.headers();
            String requestId = headers.get(HEADER_AWS_REQUEST_ID);
            if (requestId == null) {
                throw new LambdaRapidClientException("Request ID absent");
            }
            String invokedFunctionArn = headers.get(HEADER_INVOKED_FUNCTION_ARN);
            if (invokedFunctionArn == null) {
                throw new LambdaRapidClientException("Function ARN absent");
            }
            long deadlineTimeInMs = Long.parseLong(headers.get(HEADER_DEADLINE_MS, "0"));
            String xrayTraceId = headers.get(HEADER_TRACE_ID);
            String clientContext = headers.get(HEADER_CLIENT_CONTEXT);
            String cognitoIdentity = headers.get(HEADER_COGNITO_IDENTITY);
            byte[] content = new byte[response.content().readableBytes()];
            response.content().readBytes(content);

            return InvocationRequest.builder()
                    .withId(requestId)
                    .withInvokedFunctionArn(invokedFunctionArn)
                    .withDeadlineTimeInMs(deadlineTimeInMs)
                    .withXrayTraceId(xrayTraceId)
                    .withClientContext(clientContext)
                    .withCognitoIdentity(cognitoIdentity)
                    .withContent(content)
                    .build();
        } finally {
            response.release();
        }
    }

    @Override
    public void reportInvocationSuccess(String requestId, byte[] response) {
        String path = invocationPathPrefix + requestId + "/response";
        int len = response != null ? response.length : 0;
        log.message(() -> "reportInvocationSuccess POST " + path + " bodyLen=" + len);
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, path,
                Unpooled.wrappedBuffer(response != null ? response : new byte[0]));
        request.headers().set(HttpHeaderNames.CONTENT_LENGTH, request.content().readableBytes());

        FullHttpResponse resp = execute(request);
        log.message(() -> "reportInvocationSuccess response status=" + resp.status().code());
        resp.release();
        if (resp.status().code() != HTTP_OK && resp.status().code() != HTTP_ACCEPTED) {
            throw new LambdaRapidClientException(path, resp.status().code());
        }
    }

    @Override
    public void reportInvocationError(String requestId, LambdaError error) {
        String path = invocationPathPrefix + requestId + "/error";
        reportLambdaError(path, error);
    }

    @Override
    public void restoreNext() {
        HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, PATH_RESTORE_NEXT);

        FullHttpResponse response = execute(request);
        try {
            if (response.status().code() != HTTP_OK) {
                throw new LambdaRapidClientException(PATH_RESTORE_NEXT, response.status().code());
            }
        } finally {
            response.release();
        }
    }

    @Override
    public void reportRestoreError(LambdaError error) {
        reportLambdaError(PATH_RESTORE_ERROR, error);
    }

    @Override
    public StreamingResponseHandle startStreamingResponse(String requestId) {
        String path = invocationPathPrefix + requestId + "/response";
        RicLog.getLogger("streaming").message(() -> "startStreamingResponse POST " + path + " (chunked)");
        Channel ch = getChannel();

        return new NettyStreamingResponseHandle(
                ch, path, runtimeApiHost, USER_AGENT, responseHandler::prepareStreamingResponseWait);
    }

    private void reportLambdaError(String path, LambdaError error) {
        byte[] payload = JsonSerializer.serialize(error.errorRequest());
        DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, path, Unpooled.wrappedBuffer(payload));
        HttpHeaders headers = request.headers();
        headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json")
                .set(HttpHeaderNames.CONTENT_LENGTH, payload.length);

        if (error.xRayErrorCause() != null) {
            byte[] xRayJson = JsonSerializer.serialize(error.xRayErrorCause());
            if (xRayJson.length < XRAY_ERROR_CAUSE_MAX_HEADER_SIZE) {
                headers.set(HEADER_XRAY_ERROR_CAUSE, new String(xRayJson, StandardCharsets.UTF_8));
            }
        }
        if (error.errorRequest().errorType() != null) {
            headers.set(HEADER_ERROR_TYPE, error.errorRequest().errorType());
        }

        FullHttpResponse response = execute(request);
        try {
            if (response.status().code() != HTTP_ACCEPTED) {
                throw new LambdaRapidClientException(path, response.status().code());
            }
        } finally {
            response.release();
        }
    }

    private static class ResponseCaptureHandler extends ChannelInboundHandlerAdapter {
        private volatile CompletableFuture<FullHttpResponse> pendingFuture;
        private volatile CompletableFuture<FullHttpResponse> streamingResponseFuture;

        void setPendingFuture(CompletableFuture<FullHttpResponse> future) {
            this.pendingFuture = future;
        }

        CompletableFuture<FullHttpResponse> prepareStreamingResponseWait() {
            CompletableFuture<FullHttpResponse> future = new CompletableFuture<>();
            this.streamingResponseFuture = future;
            return future;
        }

        private CompletableFuture<FullHttpResponse> takePendingFuture() {
            CompletableFuture<FullHttpResponse> f = null;

            if (pendingFuture != null) {
                f = pendingFuture;
                pendingFuture = null;
            } else if (streamingResponseFuture != null) {
                f = streamingResponseFuture;
                streamingResponseFuture = null;
            }
            return f;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof FullHttpResponse response) {
                CompletableFuture<FullHttpResponse> f = takePendingFuture();
                if (f != null) {
                    f.complete(response.retain());
                } else {
                    response.release();
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        }
    }
}
