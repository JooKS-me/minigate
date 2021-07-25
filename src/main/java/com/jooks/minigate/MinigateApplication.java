package com.jooks.minigate;

import com.jooks.minigate.inbound.HttpInboundServer;
import com.jooks.minigate.router.RouterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class MinigateApplication implements ApplicationRunner {

    public static void main(String[] args) {
        SpringApplication.run(MinigateApplication.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        int proxyPort = Integer.parseInt(args.getSourceArgs()[0] == null ? "8888" : args.getSourceArgs()[0]);
        RouterRegistry.getInstance().setup();

        log.info("Mini Gate is starting...");
        HttpInboundServer server = new HttpInboundServer(proxyPort);
        try {
            server.run();
        }catch (Exception ex){
            log.error("Mini Gate started failed.");
            ex.printStackTrace();
        }
    }
}
