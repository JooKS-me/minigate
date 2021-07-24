package com.jooks.minigate;

import com.jooks.minigate.inbound.HttpInboundServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
@Slf4j
public class MinigateApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(MinigateApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String proxyPort = System.getProperty("proxyPort","8888");

        String proxyServers = System.getProperty("proxyServer","http://localhost:8080/test");

        int port = Integer.parseInt(proxyPort);
        log.info("Mini Gate is starting...");
        HttpInboundServer server = new HttpInboundServer(port, Arrays.asList(proxyServers.split(",")));
        try {
            server.run();
        }catch (Exception ex){
            log.error("Mini Gate started failed.");
            ex.printStackTrace();
        }
    }
}
