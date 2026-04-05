package com.smirnoal.crema.rapid.client;

import com.smirnoal.crema.log.RicLog;
import com.smirnoal.crema.log.RicLog.RicLogger;
import com.smirnoal.crema.rapid.client.dto.ErrorRequest;
import com.smirnoal.crema.rapid.client.serde.JsonSerializer;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.FullHttpResponse;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static com.smirnoal.crema.rapid.client.RuntimeApiConstants.*;
import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_OK;

final class NettyStreamingResponseHandle implements StreamingResponseHandle {

    private static final RicLogger log = RicLog.getLogger("streaming");

    private static final int STREAMING_RESPONSE_TIMEOUT_SECONDS = (int) REQUEST_TIMEOUT.toSeconds();

    private final Channel channel;
    private final NettyResponseStream responseStream;
    private final Supplier<CompletableFuture<FullHttpResponse>> prepareResponseWait;
    private volatile boolean finished;

    NettyStreamingResponseHandle(
            Channel channel,
            String path,
            String host,
            String userAgent,
            Supplier<CompletableFuture<FullHttpResponse>> prepareResponseWait) {
        this.channel = channel;
        this.responseStream = new NettyResponseStream(channel, path, host, userAgent);
        this.prepareResponseWait = prepareResponseWait;
    }

    @Override
    public NettyResponseStream responseStream() {
        return responseStream;
    }

    private void waitFor202() throws IOException {
        CompletableFuture<FullHttpResponse> future = prepareResponseWait.get();
        try {
            FullHttpResponse response = future.get(STREAMING_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try {
                int code = response.status().code();
                log.message(() -> "waitFor202 status=" + code);
                if (code != HTTP_ACCEPTED && code != HTTP_OK) {
                    throw new LambdaRapidClientException("Streaming response failed: " + code, code);
                }
            } finally {
                response.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for streaming response", e);
        } catch (ExecutionException e) {
            throw new IOException("Streaming response failed", e.getCause());
        } catch (TimeoutException e) {
            throw new IOException("Streaming response timed out", e);
        }
    }

    @Override
    public void complete() throws IOException {
        if (finished) return;
        finished = true;
        log.message("complete() sending empty last content");
        responseStream.ensureStarted();
        channel.writeAndFlush(DefaultLastHttpContent.EMPTY_LAST_CONTENT);
        waitFor202();
    }

    @Override
    public void fail(LambdaError error) throws IOException {
        if (finished) return;
        finished = true;

        ErrorRequest req = error.errorRequest();
        log.message(() -> "fail() sending error trailers errorType=" + (req.errorType() != null ? req.errorType() : "java.lang.Throwable"));
        String errorType = req.errorType() != null ? req.errorType() : "java.lang.Throwable";
        byte[] errorBodyJson = JsonSerializer.serialize(req);
        String errorBodyBase64 = Base64.getEncoder().encodeToString(errorBodyJson);

        responseStream.ensureStarted();

        DefaultLastHttpContent lastContent = new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER);
        lastContent.trailingHeaders()
                .set(TRAILER_ERROR_TYPE, errorType)
                .set(TRAILER_ERROR_BODY, errorBodyBase64);

        channel.writeAndFlush(lastContent);
        waitFor202();
    }
}
