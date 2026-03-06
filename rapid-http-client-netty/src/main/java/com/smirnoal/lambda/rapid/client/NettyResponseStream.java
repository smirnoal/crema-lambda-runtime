package com.smirnoal.lambda.rapid.client;

import com.smirnoal.lambda.stream.ResponseStream;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultHttpContent;

import java.io.IOException;

final class NettyResponseStream extends ResponseStream {

    private static final String STATUS_READY = "ready";
    private static final String STATUS_WRITTEN = "written";

    private final Channel channel;
    private volatile String contentType = "application/octet-stream";
    private volatile String status = STATUS_READY;

    NettyResponseStream(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void setContentType(String contentType) {
        if (!STATUS_READY.equals(status)) {
            throw new IllegalStateException("Cannot set content-type after first write");
        }
        this.contentType = contentType;
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
        if (len <= 0) return;
        status = STATUS_WRITTEN;

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

    String getContentType() {
        return contentType;
    }
}
