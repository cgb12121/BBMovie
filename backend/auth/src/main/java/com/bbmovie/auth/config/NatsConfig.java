package com.bbmovie.auth.config;

import com.bbmovie.auth.dto.event.NatsConnectionEvent;
import io.nats.client.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Configuration
public class NatsConfig {

    @Bean
    public NatsConnectionFactory natsConnectionFactory(ApplicationEventPublisher publisher) {
        Options options = new Options.Builder()
                .server("nats://localhost:4222")
                .connectionName("auth-service")
                .maxReconnects(-1)
                .reconnectWait(Duration.ofSeconds(2))
                .connectionTimeout(Duration.ofSeconds(5))
                .pingInterval(Duration.ofSeconds(10))
                .connectionListener((conn, type) -> {
                    switch (type) {
                        case CONNECTED, RECONNECTED -> {
                            log.info("NATS Connected/Reconnected, publishing event.");
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
                })
                .build();
        return new NatsConnectionFactory(options);
    }

    public static class NatsConnectionFactory implements SmartLifecycle {

        private final Options options;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicReference<Connection> connectionRef = new AtomicReference<>();
        private final CountDownLatch connectionLatch = new CountDownLatch(1);

        public NatsConnectionFactory(Options options) {
            this.options = options;
        }

        public Connection getConnection() {
            try {
                if (!connectionLatch.await(1, TimeUnit.SECONDS)) { // Wait for a short time
                    log.warn("NATS connection not yet available.");
                    return null; // Return null if not connected, consumers must handle this
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for NATS connection.");
                return null;
            }
            return connectionRef.get();
        }

        @Override
        public void start() {
            if (running.compareAndSet(false, true)) {
                Thread.ofVirtual().start(() -> {
                    log.info("Starting NATS connection attempt in background...");
                    try {
                        Connection nc = Nats.connect(options);
                        connectionRef.set(nc);
                        log.info("Successfully connected to NATS in background.");
                    } catch (IOException | InterruptedException e) {
                        log.error("Failed to establish initial NATS connection in background. Will rely on auto-reconnect.", e);
                    } finally {
                        connectionLatch.countDown(); // Signal that the connection attempt is complete
                    }
                });
            }
        }

        @Override
        public void stop() {
            if (running.compareAndSet(true, false)) {
                Connection conn = connectionRef.get();
                if (conn != null) {
                    try {
                        conn.close();
                        log.info("NATS connection closed");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Error closing NATS connection: {}", e.getMessage());
                    }
                }
            }
        }

        @Override
        public boolean isRunning() {
            return running.get();
        }
    }
}