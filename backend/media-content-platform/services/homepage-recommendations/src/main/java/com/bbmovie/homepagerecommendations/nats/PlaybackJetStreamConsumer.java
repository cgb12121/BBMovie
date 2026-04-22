package com.bbmovie.homepagerecommendations.nats;

import com.bbmovie.homepagerecommendations.config.HomepageRecommendationsProperties;
import com.bbmovie.homepagerecommendations.dto.PlaybackAnalyticsEvent;
import com.bbmovie.homepagerecommendations.service.TrendingWriteBatcher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "homepage.recommendations.nats.enabled", havingValue = "true")
public class PlaybackJetStreamConsumer implements ApplicationRunner {

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final TrendingWriteBatcher trendingWriteBatcher;
    private final HomepageRecommendationsProperties properties;

    private final ExecutorService pullExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "homepage-nats-pull");
        t.setDaemon(true);
        return t;
    });
    private final AtomicBoolean running = new AtomicBoolean(true);
    private JetStreamSubscription subscription;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String stream = properties.getNats().getStreamName();
        String subject = properties.getNats().getSubject();
        String durable = properties.getNats().getConsumerDurable();

        ensureStream(stream, subject);

        JetStream js = connection.jetStream();
        PullSubscribeOptions opts = PullSubscribeOptions.builder().durable(durable).build();
        subscription = js.subscribe(subject, opts);

        log.info("JetStream pull consumer started: stream subject={}, durable={}", subject, durable);

        pullExecutor.submit(this::pullLoop);
    }

    /**
     * Dev convenience only; production streams/consumers should be provisioned via IaC or ops scripts
     * (MaxDeliver, DLQ, replicas, etc.).
     */
    private void ensureStream(String streamName, String subject) {
        try {
            connection
                    .jetStreamManagement()
                    .addStream(StreamConfiguration.builder()
                            .name(streamName)
                            .subjects(subject)
                            .storageType(StorageType.File)
                            .build());
            log.info("Created JetStream stream {} for subject {}", streamName, subject);
        } catch (JetStreamApiException e) {
            if (e.getApiErrorCode() == 10058) {
                log.debug("JetStream stream {} already exists", streamName);
            } else {
                log.warn("JetStream addStream {}: {}", streamName, e.getMessage());
            }
        } catch (IOException e) {
            log.warn("JetStream stream setup failed: {}", e.getMessage());
        }
    }

    private void pullLoop() {
        while (running.get() && subscription != null && subscription.isActive()) {
            try {
                List<Message> messages = subscription.fetch(32, Duration.ofSeconds(2));
                for (Message msg : messages) {
                    handle(msg);
                }
            } catch (IllegalStateException ex) {
                if (!running.get()) {
                    break;
                }
                log.debug("Pull loop: {}", ex.getMessage());
            } catch (Exception ex) {
                if (running.get()) {
                    log.warn("Playback pull error: {}", ex.getMessage());
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void handle(Message msg) {
        try {
            PlaybackAnalyticsEvent event = objectMapper.readValue(msg.getData(), PlaybackAnalyticsEvent.class);
            trendingWriteBatcher.record(event);
            msg.ack();
        } catch (Exception e) {
            log.warn("Failed playback event: {}", e.getMessage());
            msg.nak();
        }
    }

    @PreDestroy
    void shutdown() {
        running.set(false);
        if (subscription != null) {
            try {
                subscription.unsubscribe();
            } catch (Exception e) {
                log.debug("Unsubscribe: {}", e.getMessage());
            }
        }
        pullExecutor.shutdownNow();
    }
}
