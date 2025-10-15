package com.bbmovie.search.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.bbmovie.search.service.nats.HealthEventProducer;
import com.example.common.dtos.events.SystemHealthEvent;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Log4j2
public class ElasticsearchHealthCheckService {

    private final ElasticsearchClient elasticsearchClient;
    private final HealthEventProducer healthEventProducer;
    private boolean isDown = false;

    public ElasticsearchHealthCheckService(ElasticsearchClient elasticsearchClient, HealthEventProducer healthEventProducer) {
        this.elasticsearchClient = elasticsearchClient;
        this.healthEventProducer = healthEventProducer;
    }

    @PostConstruct
    public void startHealthChecks() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(this::runHealthChecks);
    }

    private void runHealthChecks() {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(Integer.MAX_VALUE) // Keep retrying indefinitely
                .intervalFunction(IntervalFunction.ofExponentialBackoff(10000, 2.0)) // 10s, 20s, 40s, ...
                .build();

        Retry retry = Retry.of("elasticsearch-health-check", retryConfig);

        Callable<Void> healthCheckCallable = () -> {
            checkHealth();
            return null;
        };

        Callable<Void> decoratedHealthCheck = Retry.decorateCallable(retry, healthCheckCallable);

        try {
            decoratedHealthCheck.call();
        } catch (Exception e) {
            log.error("Health check retry loop failed", e);
        }
    }

    private void checkHealth() throws IOException {
        try {
            boolean isConnected = elasticsearchClient.ping().value();

            if (isConnected) {
                handleSuccessfulCheck();
            } else {
                handleFailedCheck("Ping returned false, cluster is likely unhealthy.");
            }
        } catch (IOException e) {
            handleFailedCheck("Failed to connect to Elasticsearch: " + e.getMessage());
            throw e; // Re-throw to trigger retry
        }
    }

    private void handleSuccessfulCheck() {
        if (isDown) {
            log.info("Connection to Elasticsearch has been RESTORED.");
            healthEventProducer.publishHealthEvent(SystemHealthEvent.builder()
                    .service("elasticsearch")
                    .status(SystemHealthEvent.Status.UP)
                    .reason("Connection restored")
                    .build());
            isDown = false;
        }
        log.debug("Elasticsearch health check successful.");
    }

    private void handleFailedCheck(String reason) {
        log.warn("Elasticsearch health check failed. Reason: {}", reason);
        if (!isDown) {
            isDown = true;
            log.error("CRITICAL: Elasticsearch is DOWN.");
            healthEventProducer.publishHealthEvent(SystemHealthEvent.builder()
                    .service("elasticsearch")
                    .status(SystemHealthEvent.Status.DOWN)
                    .reason(reason)
                    .build());
        }
    }
}