package com.bbmovie.search.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.bbmovie.search.dto.event.ElasticsearchDownEvent;
import com.bbmovie.search.dto.event.ElasticsearchUpEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Log4j2
public class ElasticsearchHealthCheckService {

    private final ElasticsearchClient elasticsearchClient;
    private final ApplicationEventPublisher eventPublisher;
    private boolean isUp = true; // Assume the connection is up initially

    @Autowired
    public ElasticsearchHealthCheckService(ElasticsearchClient elasticsearchClient, ApplicationEventPublisher eventPublisher) {
        this.elasticsearchClient = elasticsearchClient;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(
            fixedRateString = "${elasticsearch.health.check.rate:5000}",
            initialDelay = 5000
    )
    public void checkHealth() {
        try {
            boolean currentStatus = elasticsearchClient.ping().value();

            if (currentStatus) {
                handleSuccessfulCheck();
            } else {
                handleFailedCheck("Ping returned false, cluster is likely unhealthy.");
            }
        } catch (IOException e) {
            handleFailedCheck("Failed to connect to Elasticsearch: " + e.getMessage());
        }
    }

    private void handleSuccessfulCheck() {
        if (!isUp) {
            isUp = true;
            log.info("Connection to Elasticsearch has been RESTORED.");
            eventPublisher.publishEvent(new ElasticsearchUpEvent(this));
        }
        log.debug("Elasticsearch health check successful.");
    }

    private void handleFailedCheck(String reason) {
        if (isUp) {
            isUp = false;
            log.warn("Elasticsearch health check failed. Reason: {}", reason);
            eventPublisher.publishEvent(new ElasticsearchDownEvent(this));
        }
    }
}