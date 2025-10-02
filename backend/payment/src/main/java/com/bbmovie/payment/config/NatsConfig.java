package com.bbmovie.payment.config;

import com.bbmovie.payment.dto.event.NatsConnectionEvent;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.nats.client.*;
import io.nats.client.api.*;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Configuration
public class NatsConfig {

    private static final String paymentStream = "PAYMENTS";

    private final ApplicationEventPublisher publisher;

    @Autowired
    public NatsConfig(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Bean
    public NatsConnectionFactory natsConnection() {
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
                            log.info("[{}], retrying setup & resubscribing", type.getEvent());
                            publisher.publishEvent(new NatsConnectionEvent(conn, type));
                        }
                        case DISCONNECTED -> log.warn("Disconnected from NATS");
                        case CLOSED -> log.error("Connection to NATS closed");
                    }
                })
                .build();
        return new NatsConnectionFactory(options);
    }

    public static class NatsConnectionFactory implements SmartLifecycle {

        private final Options options;
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final AtomicBoolean running = new AtomicBoolean(false);

        @Getter
        private volatile Connection connection;

        public NatsConnectionFactory(Options options) {
            this.options = options;
        }

        @Override
        public void start() {
            if (running.compareAndSet(false, true)) {
                executor.submit(this::connectWithRetry);
            }
        }

        private void connectWithRetry() {
            RetryConfig config = RetryConfig.custom()
                    .maxAttempts(Integer.MAX_VALUE)
                    .waitDuration(Duration.ofSeconds(2)) // base wait
                    .intervalFunction(IntervalFunction.ofExponentialBackoff(2000, 2.0, 30000))
                    .retryExceptions(Exception.class) // retry on all NATS connect errors
                    .build();

            Retry retry = Retry.of("nats-connect", config);

            Callable<Connection> connect = Retry.decorateCallable(retry, () -> {
                log.info("Trying to connect to NATS...");
                Connection conn = Nats.connect(options);
                setup(conn);
                return conn;
            });

            while (running.get() && connection == null) {
                try {
                    this.connection = connect.call();
                    log.info("Successfully connected to NATS");
                    break;
                } catch (Exception e) {
                    log.error("Failed to connect to NATS, will retry", e);
                }
            }
        }

        @Override
        public void stop() {
            if (running.compareAndSet(true, false)) {
                if (connection != null) {
                    try {
                        connection.close();
                        log.info("NATS connection closed");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Error closing NATS connection", e);
                    }
                }
            }
        }

        @Override
        public boolean isRunning() {
            return running.get();
        }

        private void setup(Connection connection) throws IOException {
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
            }
        }
    }
}