package com.bbmovie.payment.config;

import com.bbmovie.payment.dto.event.NatsConnectionEvent;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.nats.client.*;
import io.nats.client.api.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Log4j2
@Configuration
public class NatsConfig {

    @Value("${nats.js.stream.payment}")
    private String paymentStream;

    private final AtomicBoolean streamInitialized = new AtomicBoolean(false);

    private Connection connection;

    @Bean
    public SmartLifecycle natsLifecycle(NatsConnectionFactory factory) {
        return factory;
    }

    @Bean
    public NatsConnectionFactory natsConnection(ApplicationEventPublisher publisher) {
        Options options = new Options.Builder()
                .server("nats://localhost:4222")
                .connectionName("payment-service")
                .maxReconnects(-1)
                .reconnectWait(Duration.ofSeconds(10))
                .connectionTimeout(Duration.ofSeconds(5))
                .pingInterval(Duration.ofSeconds(30))
                .connectionListener((conn, type) -> {
                    switch (type) {
                        case CONNECTED, RECONNECTED -> {
                            log.info("[{}], (re)subscribing consumers", type.getEvent());
                            publisher.publishEvent(new NatsConnectionEvent(this, conn, type));
                        }
                        case DISCONNECTED -> log.warn("Disconnected from NATS");
                        case CLOSED -> log.error("Connection to NATS closed");
                        default -> log.info("NATS connection event: {}", type);
                    }
                })
                .build();
        return new NatsConnectionFactory(options);
    }

    @EventListener
    public void onApplicationEvent(@NonNull NatsConnectionEvent event) {
        if (streamInitialized.compareAndSet(false, true)) {
            try {
                this.connection = event.connection();
                setupStream();
                log.info("JetStream initialized after NATS connection");
            } catch (IOException e) {
                log.error("Failed to setup NATS stream: {}", e.getMessage());
                streamInitialized.set(false);
            }
        }
    }

    private void setupStream() throws IOException {
        if (streamInitialized.get()) {
            return;
        }

        JetStreamManagement jsm = connection.jetStreamManagement();
        try {
            StreamInfo stream = jsm.getStreamInfo(paymentStream);
            log.info("Stream already exists {}", stream.toString());
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                try {
                    StreamConfiguration streamConfig = StreamConfiguration.builder()
                            .name(paymentStream)
                            .subjects("payments.*")
                            .storageType(StorageType.Memory)
                            .retentionPolicy(RetentionPolicy.WorkQueue)
                            .build();
                    jsm.addStream(streamConfig);
                    log.info("Created stream: {}", paymentStream);
                } catch (JetStreamApiException ex) {
                    log.error("Error creating stream: {}", paymentStream, ex);
                }
            } else {
                log.error("Unable to create stream {}", paymentStream, e);
            }
        } catch (Exception e) {
            log.error("Unable to create stream {}", paymentStream, e);
        } finally {
            streamInitialized.set(true);
        }
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
                    .maxAttempts(Integer.MAX_VALUE)
                    .intervalFunction(
                            IntervalFunction.ofExponentialBackoff(2000, 2.0, 30000)
                    )
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
    }
}