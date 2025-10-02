package com.bbmovie.auth.config;

import com.bbmovie.auth.dto.event.NatsConnectionEvent;
import io.nats.client.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Log4j2
@Configuration
public class NatsConfig {

    private final ApplicationEventPublisher publisher;

    @Autowired
    public NatsConfig(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Bean
    public Connection natsConnection() throws IOException, InterruptedException {
        Options options = new Options.Builder()
                .server("nats://localhost:4222")
                .connectionName("auth-service")
                .maxReconnects(-1) // infinitely reconnects
                .reconnectWait(Duration.ofSeconds(2))
                .connectionTimeout(Duration.ofSeconds(5))
                .pingInterval(Duration.ofSeconds(10))
                .connectionListener((conn, type) -> {
                    switch (type) {
                        case CONNECTED, RECONNECTED -> {
                            log.info("Connected/Reconnected, (re)subscribing consumers");
                            publisher.publishEvent(new NatsConnectionEvent(conn, type));
                        }
                        case DISCONNECTED -> log.warn("Disconnected from NATS");
                        case CLOSED -> log.error("Connection to NATS closed");
                        default -> log.info("NATS connection event: {}", type);
                    }
                })
                .errorListener(new ErrorListener() {
                    @Override
                    public void errorOccurred(Connection conn, String error) {
                        log.error("NATS error: {}", error);
                    }

                    @Override
                    public void exceptionOccurred(Connection conn, Exception exp) {
                        log.error("NATS exception", exp);
                    }

                    @Override
                    public void slowConsumerDetected(Connection conn, Consumer consumer) {
                        log.warn("Slow consumer detected: {}", consumer);
                    }
                })
                .build();
        return Nats.connect(options);
    }
}