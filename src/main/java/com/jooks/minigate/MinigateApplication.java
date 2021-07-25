package com.jooks.minigate;

import com.jooks.minigate.inbound.HttpInboundServer;
import com.jooks.minigate.router.RouterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class MinigateApplication implements ApplicationRunner {

    @Value("${minigate.server.port}")
    private int proxyPort;

    @Value("${minigate.balance:random}")
    private String balance;

    public static void main(String[] args) {
        SpringApplication.run(MinigateApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        RouterRegistry.getInstance().setup();

        log.info("Mini Gate is starting...");
        HttpInboundServer server = new HttpInboundServer(proxyPort, balance);
        try {
            server.run();
        }catch (Exception ex){
            log.error("Mini Gate started failed.");
            ex.printStackTrace();
        }
    }
}
