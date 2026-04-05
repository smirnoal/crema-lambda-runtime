package com.smirnoal.crema.rapid.client;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Netty-based HTTP test server for testing the Netty rapid client.
 * Handles chunked requests with trailers (unlike OkHttp MockWebServer).
 */
final class NettyTestServer {

    private static final int MAX_CONTENT_LENGTH = 65536;

    private final Queue<TestResponse> responseQueue = new ArrayDeque<>();
    private final BlockingQueue<RecordedRequest> requestQueue = new LinkedBlockingQueue<>();
    private final EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
    private final EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

    private volatile Channel serverChannel;
    private volatile String host;
    private volatile int port;

    void start() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new ReadTimeoutHandler(30), new WriteTimeoutHandler(30))
                                .addLast(new HttpServerCodec())
                                .addLast(new HttpObjectAggregator(MAX_CONTENT_LENGTH))
                                .addLast(new TestServerHandler(responseQueue, requestQueue));
                    }
                })
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        ChannelFuture bindFuture = bootstrap.bind(0);
        bindFuture.sync();
        serverChannel = bindFuture.channel();
        InetSocketAddress addr = (InetSocketAddress) serverChannel.localAddress();
        host = addr.getHostString();
        port = addr.getPort();
    }

    void shutdown() {
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
        }
        workerGroup.shutdownGracefully().syncUninterruptibly();
        bossGroup.shutdownGracefully().syncUninterruptibly();
    }

    String getHostName() {
        return host;
    }

    int getPort() {
        return port;
    }

    void enqueue(TestResponse response) {
        responseQueue.add(response);
    }

    RecordedRequest takeRequest(long timeout, TimeUnit unit) throws InterruptedException {
        RecordedRequest req = requestQueue.poll(timeout, unit);
        if (req == null) {
            throw new AssertionError("No request received within " + timeout + " " + unit);
        }
        return req;
    }

    record RecordedRequest(String path, String method, HttpHeaders headers, byte[] body) {
        String getPath() {
            return path;
        }

        String getHeader(String name) {
            return headers.get(name);
        }

        String getBodyUtf8() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    record TestResponse(int statusCode, HttpHeaders headers, byte[] body) {
        static TestResponse of(int statusCode) {
            return new TestResponse(statusCode, new DefaultHttpHeaders(), new byte[0]);
        }

        TestResponse addHeader(String name, String value) {
            DefaultHttpHeaders h = new DefaultHttpHeaders();
            h.add(headers);
            h.add(name, value);
            return new TestResponse(statusCode, h, body);
        }

        TestResponse withBody(String body) {
            return new TestResponse(statusCode, headers, body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static final class TestServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        private final Queue<TestResponse> responseQueue;
        private final BlockingQueue<RecordedRequest> requestQueue;

        TestServerHandler(Queue<TestResponse> responseQueue, BlockingQueue<RecordedRequest> requestQueue) {
            this.responseQueue = responseQueue;
            this.requestQueue = requestQueue;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
            HttpMethod method = req.method();
            String path = new QueryStringDecoder(req.uri()).path();
            ByteBuf content = req.content();
            byte[] body = new byte[content.readableBytes()];
            content.readBytes(body);

            requestQueue.add(new RecordedRequest(path, method.name(), req.headers().copy(), body));

            TestResponse resp = responseQueue.poll();
            if (resp == null) {
                resp = TestResponse.of(200);
            }

            FullHttpResponse response = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    HttpResponseStatus.valueOf(resp.statusCode()),
                    Unpooled.wrappedBuffer(resp.body()));
            response.headers()
                    .set(HttpHeaderNames.CONTENT_LENGTH, resp.body().length)
                    .add(resp.headers());

            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
