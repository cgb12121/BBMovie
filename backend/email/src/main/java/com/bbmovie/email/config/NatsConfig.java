package com.bbmovie.email.config;

import com.bbmovie.email.dto.event.NatsConnectionEvent;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.nats.client.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

@Log4j2
@Configuration
public class NatsConfig {

    @Bean
    public SmartLifecycle natsConnectionLifecycle(NatsConnectionFactory factory) {
        return factory; // force lifecycle registration
    }

    @Bean
    public NatsConnectionFactory natsConnectionFactory(ApplicationEventPublisher publisher) {
        Options options = new Options.Builder()
                .server("nats://localhost:4222")
                .connectionName("email-service")
                .maxReconnects(-1) // infinitely reconnects
                .reconnectWait(Duration.ofSeconds(10))
                .connectionTimeout(Duration.ofSeconds(5))
                .pingInterval(Duration.ofSeconds(30))
                .connectionListener((conn, type) -> {
                    switch (type) {
                        case CONNECTED, RECONNECTED -> {
                            log.info("[{}], (re)subscribing consumers", type.getEvent());
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
                        log.error("NATS exception happened [{}]: {}", exp.getClass().getName(), exp.getMessage());
                    }

                    @Override
                    public void slowConsumerDetected(Connection conn, Consumer consumer) {
                        log.warn("Slow consumer detected: {}", consumer);
                    }
                })
                .build();

        return new NatsConnectionFactory(options);
    }

    public static class NatsConnectionFactory implements SmartLifecycle {

        private final Options options;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicReference<Connection> connectionAtomicReference = new AtomicReference<>();

        public NatsConnectionFactory(Options options) {
            this.options = options;
        }

        public Connection getConnection() {
            return connectionAtomicReference.get();
        }

        @Override
        public void start() {
            log.info("Starting NATS connection lifecycle...");
            if (running.compareAndSet(false, true)) {
                executor.submit(() -> {
                    try {
                        this.connectWithRetry();
                    } catch (Exception e) {
                        log.error("NATS connection thread crashed [{}]: {}", e.getClass().getName(), e.getMessage());
                    }
                });

            }
        }

        private void connectWithRetry() {
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(Integer.MAX_VALUE) // keep retrying forever
                    .intervalFunction(
                            IntervalFunction.ofExponentialBackoff(2000, 2.0, 30000)
                    ) // 2s -> 4s -> 8s -> ... capped at 30s
                    .retryExceptions(Exception.class) // retry on all NATS connect errors
                    .build();

            Retry retry = Retry.of("nats-connect", config);

            Callable<Connection> connect = Retry.decorateCallable(retry, () -> {
                log.info("Trying to connect to NATS...");
                return Nats.connect(options);
            });

            while (running.get() && connectionAtomicReference.get() == null) {
                try {
                    this.connectionAtomicReference.set(connect.call());
                    log.info("Successfully connected to NATS");
                    break;
                } catch (Exception e) {
                    log.error("Failed to connect to NATS, will retry: {}", e.getMessage());
                }
            }
        }

        @Override
        public void stop() {
            if (running.compareAndSet(true, false)) {
                Connection conn = connectionAtomicReference.get();
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

        @Override
        public boolean isAutoStartup() {
            return true;
        }
    }
}