package com.smirnoal.crema.rapid.client;

import com.smirnoal.crema.stream.ResponseStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.io.IOException;

import static com.smirnoal.crema.rapid.client.RuntimeApiConstants.HEADER_RESPONSE_MODE;
import static com.smirnoal.crema.rapid.client.RuntimeApiConstants.TRAILER_ERROR_BODY;
import static com.smirnoal.crema.rapid.client.RuntimeApiConstants.TRAILER_ERROR_TYPE;
import static com.smirnoal.crema.rapid.client.RuntimeApiConstants.VALUE_STREAMING;

final class NettyResponseStream extends ResponseStream {

    private static final String STATUS_READY = "ready";
    private static final String STATUS_WRITTEN = "written";

    private final Channel channel;
    private final String path;
    private final String host;
    private final String userAgent;
    private volatile String contentType = "application/octet-stream";
    private volatile String status = STATUS_READY;

    NettyResponseStream(Channel channel, String path, String host, String userAgent) {
        this.channel = channel;
        this.path = path;
        this.host = host;
        this.userAgent = userAgent;
    }

    @Override
    public void setContentType(String contentType) {
        if (!STATUS_READY.equals(status)) {
            throw new IllegalStateException("Cannot set content-type after first write");
        }
        this.contentType = contentType;
    }

    /**
     * Sends the HTTP request line and headers on first use so {@link #setContentType(String)} can
     * take effect before the POST body is started.
     */
    void ensureStarted() {
        if (!STATUS_READY.equals(status)) {
            return;
        }
        status = STATUS_WRITTEN;
        HttpHeaders headers = new DefaultHttpHeaders()
                .set(HEADER_RESPONSE_MODE, VALUE_STREAMING)
                .set(HttpHeaderNames.HOST, host)
                .set(HttpHeaderNames.USER_AGENT, userAgent)
                .set(HttpHeaderNames.TRANSFER_ENCODING, "chunked")
                .set(HttpHeaderNames.CONTENT_TYPE, contentType)
                .set(HttpHeaderNames.TRAILER, TRAILER_ERROR_TYPE + ", " + TRAILER_ERROR_BODY);

        DefaultHttpRequest request = new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path, headers);
        channel.writeAndFlush(request);
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b});
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len <= 0) {
            return;
        }
        ensureStarted();

        ByteBuf buf = Unpooled.wrappedBuffer(b, off, len);
        ChannelFuture future = channel.writeAndFlush(new DefaultHttpContent(buf));

        if (!channel.isWritable()) {
            try {
                future.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while writing", e);
            }
        }
    }

    @Override
    public void flush() throws IOException {
        channel.flush();
    }

    @Override
    public void close() {
        // No-op: lifecycle managed by StreamingResponseHandle.complete() or fail()
    }
}
