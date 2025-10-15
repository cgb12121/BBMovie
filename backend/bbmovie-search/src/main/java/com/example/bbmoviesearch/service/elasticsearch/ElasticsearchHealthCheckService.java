package com.example.bbmoviesearch.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Log4j2
public class ElasticsearchHealthCheckService {

    private final ElasticsearchClient elasticsearchClient;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private static final int FAILURE_THRESHOLD = 3; // Trigger alert after 3 consecutive failures
    private boolean isDown = false;

    public ElasticsearchHealthCheckService(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Scheduled(fixedRate = 10000) // Run every 10 seconds
    public void checkHealth() {
        try {
            // Use the ping API, a lightweight way to check cluster health
            boolean isConnected = elasticsearchClient.ping().value();

            if (isConnected) {
                handleSuccessfulCheck();
            } else {
                handleFailedCheck("Ping returned false, cluster is likely unhealthy.");
            }
        } catch (IOException e) {
            // IOException indicates a transport-level problem (e.g., server is down)
            handleFailedCheck("Failed to connect to Elasticsearch: " + e.getMessage());
        }
    }

    private void handleSuccessfulCheck() {
        failureCount.set(0);
        if (isDown) {
            // This is the "healing" part: log that the connection is restored
            log.info("Connection to Elasticsearch has been RESTORED.");
            // Here you could send a "RESOLVED" notification to your alerting system
            isDown = false;
        }
        log.debug("Elasticsearch health check successful.");
    }

    private void handleFailedCheck(String reason) {
        int currentFailures = failureCount.incrementAndGet();
        log.warn("Elasticsearch health check failed (Attempt {}/{}). Reason: {}", currentFailures, FAILURE_THRESHOLD, reason);

        if (currentFailures >= FAILURE_THRESHOLD && !isDown) {
            // Threshold breached, move to "down" state and send alert
            isDown = true;
            log.error("CRITICAL: Elasticsearch is DOWN. Breached failure threshold of {}.", FAILURE_THRESHOLD);
            // ** ALERTING ACTION GOES HERE **
            // Example: sendEmailNotification("Elasticsearch is down!");
        }
    }

    // Example of a placeholder for a real alerting mechanism
    /*
    private void sendEmailNotification(String message) {
        // Integration with an email service (e.g., using JavaMailSender)
        log.info("Sending email notification: {}", message);
    }
    */
}
