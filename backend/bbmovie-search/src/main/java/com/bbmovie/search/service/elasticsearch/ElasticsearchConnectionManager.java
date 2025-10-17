package com.bbmovie.search.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.bbmovie.search.dto.event.ElasticsearchDownEvent;
import com.bbmovie.search.dto.event.ElasticsearchUpEvent;
import com.bbmovie.search.service.nats.HealthEventProducer;
import com.example.common.dtos.events.SystemHealthEvent;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.Callable;

@Service
@Log4j2
public class ElasticsearchConnectionManager {

    private final ElasticsearchClient elasticsearchClient;
    private final HealthEventProducer healthEventProducer;

    @Autowired
    public ElasticsearchConnectionManager(ElasticsearchClient elasticsearchClient, HealthEventProducer healthEventProducer) {
        this.elasticsearchClient = elasticsearchClient;
        this.healthEventProducer = healthEventProducer;
    }

    @EventListener(ElasticsearchDownEvent.class)
    public void onElasticsearchDown() {
        log.error("CRITICAL: Elasticsearch is DOWN. Starting reconnection attempts...");

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(5) // Attempt to reconnect 5 times
                .intervalFunction(IntervalFunction.ofExponentialBackoff(
                        Duration.ofSeconds(10), 1.5, Duration.ofSeconds(30))
                ) // 10s, 15s, 22s..., 30s
                .retryExceptions(Exception.class)
                .build();

        Retry retry = Retry.of("es-reconnect", retryConfig);

        Callable<Boolean> reconnectCallable = () -> elasticsearchClient.ping().value();

        Callable<Boolean> decoratedReconnect = Retry.decorateCallable(retry, reconnectCallable);

        try {
            boolean reconnected = decoratedReconnect.call();
            if (!reconnected) {
                // This block will be entered if all retries fail and the last attempt also returns false
                throw new IllegalStateException("Final reconnection attempt failed.");
            }
            // If reconnected, the health checker will publish an 'Up' event, so we do nothing here.
        } catch (Exception e) {
            log.error("Could not reconnect to Elasticsearch after multiple retries. Sending failure notification.");
            healthEventProducer.publishHealthEvent(SystemHealthEvent.down(
                    "elasticsearch",
                    "Elasticsearch is DOWN. Failed to reconnect after multiple attempts."
            ));
        }
    }

    @EventListener(ElasticsearchUpEvent.class)
    public void onElasticsearchUp() {
        log.info("Publishing Elasticsearch UP event to NATS.");
        healthEventProducer.publishHealthEvent(
                SystemHealthEvent.up("elasticsearch", "Elasticsearch connection has been restored.")
        );
    }
}
