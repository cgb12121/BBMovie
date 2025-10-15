package com.example.bbmoviesearch.config.ai_vector;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
@Log4j2
public class ElasticsearchConnectionInitializer {

    private final ElasticsearchClient client;

    @Autowired
    public ElasticsearchConnectionInitializer(ElasticsearchClient client) {
        this.client = client;
    }

    @EventListener({ ApplicationReadyEvent.class })
    public void onApplicationReady() {
        waitForElasticsearch(client);
    }

    public void waitForElasticsearch(ElasticsearchClient client) {
        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(Integer.MAX_VALUE)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(3000, 2.0))
                .retryExceptions(Exception.class)
                .build();

        Retry retry = Retry.of("es-connect", retryConfig);

        Callable<Void> checkConnection = Retry.decorateCallable(retry, () -> {
            try {
                client.info();
                log.info("Connected to Elasticsearch successfully.");
                return null;
            } catch (Exception e) {
                log.warn("Elasticsearch connection failed: {}", e.getMessage());
                throw e;
            }
        });

        try {
            checkConnection.call();
        } catch (Exception e) {
            log.error("Could not connect to Elasticsearch after retries: {}", e.getMessage());
        }
    }
}
