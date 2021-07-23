package com.jooks.minigate.client;

import com.google.common.eventbus.Subscribe;
import com.jooks.minigate.message.MessageCenter;
import com.jooks.minigate.message.MessageRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;

@Component
@Slf4j
public class HttpClient implements ApplicationRunner{

    @Value("${minigate.target.url}")
    private String url;

    private Bootstrap b;

    EventLoopGroup group;

    private HttpClient() {
        MessageCenter.getInstance().getAsyncEventBus().register(this);
    }

    @Subscribe
    public void sendRequest(String channelCode) throws URISyntaxException, SSLException {
        URI uri = new URI(url);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            }

            if (!"http".equalsIgnoreCase(scheme)) {
                System.err.println("Only HTTP is supported.");
                return;
            }

            // Configure the client.
            EventLoopGroup group = new NioEventLoopGroup();
            try {
                // Make the connection attempt.
                Channel ch = b.connect(host, port).sync().channel();

                // Prepare the HTTP request.
                HttpRequest request = new DefaultFullHttpRequest(
                        HttpVersion.HTTP_1_1, HttpMethod.GET, uri.getRawPath(), Unpooled.EMPTY_BUFFER);
                request.headers().set(HttpHeaderNames.HOST, host);
                request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                request.headers().set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);

                // Set some example cookies.
                request.headers().set(
                        HttpHeaderNames.COOKIE,
                        io.netty.handler.codec.http.cookie.ClientCookieEncoder.STRICT.encode(
                                new io.netty.handler.codec.http.cookie.DefaultCookie("my-cookie", "foo"),
                                new DefaultCookie("another-cookie", "bar")));

                // Send the HTTP request.
                ch.writeAndFlush(request);

                // Wait for the server to close the connection.
                ch.closeFuture().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                // Shut down executor threads to exit.
                group.shutdownGracefully();
            }
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        b = new Bootstrap();
        group = new NioEventLoopGroup();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new HttpClientInitializer());
    }
}
