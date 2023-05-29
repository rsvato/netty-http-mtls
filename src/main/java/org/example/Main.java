package org.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

public class Main {
    public static void main(String[] args) {
        final SslContext sslContext;
        try {
            SelfSignedCertificate cert = new SelfSignedCertificate();
            sslContext = SslContextBuilder.forServer(cert.certificate(), cert.privateKey())
                    .clientAuth(ClientAuth.REQUIRE)
                    .build();
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup(2);
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new SimpleHandler(sslContext));
            Channel channel = bootstrap.bind(18443).sync().channel();
            channel.closeFuture().sync();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static class SimpleHandler extends ChannelInitializer<SocketChannel> {

        private final SslContext sslContext;

        public SimpleHandler(SslContext context) {
            this.sslContext = context;
        }

        @Override
        protected void initChannel(SocketChannel socketChannel) throws Exception {
            ChannelPipeline pipeline = socketChannel.pipeline();
            pipeline.addLast(sslContext.newHandler(socketChannel.alloc()));
            pipeline.addLast(new HttpServerCodec());
            pipeline.addLast(new HttpServerExpectContinueHandler());
        }
    }
}