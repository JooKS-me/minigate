package com.jooks.minigate.server;

import com.google.common.eventbus.Subscribe;
import com.jooks.minigate.message.HttpResponseMessage;
import com.jooks.minigate.message.MessageCenter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Server implements ApplicationRunner, DisposableBean {

    @Value("${minigate.server.port}")
    private Integer port;

    @Value("${minigate.server.bossCore}")
    private Integer bossCore;

    @Value("${minigate.server.workerCore}")
    private Integer workerCore;

    EventLoopGroup bossGroup;

    EventLoopGroup workerGroup;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        port = port == null ? 13307 : port;

        // Configure the bootstrap.
        EventLoopGroup bossGroup = new NioEventLoopGroup(bossCore);
        EventLoopGroup workerGroup = new NioEventLoopGroup(workerCore);

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ServerInitializer());

        Channel ch = b.bind(port).sync().channel();

        ch.closeFuture().sync();

        log.info("Mini Gate start on {} successfully.", port);
    }

    @Override
    public void destroy() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
