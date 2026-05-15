package bbmovie.ai_platform.agentic_ai.service.chat;

import io.nats.client.*;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * UsageWorker is a background service that processes token usage events from NATS.
 * 
 * In a production environment, this worker would persist the usage data to a 
 * database (e.g., PostgreSQL or a time-series DB like InfluxDB) for billing 
 * and analytics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsageWorker {

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    @Value("${nats.usage.subject:ai.usage.recorded}")
    private String usageSubject;

    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        log.info("[UsageWorker] Initializing JetStream Pull Worker on subject: {}", usageSubject);
        try {
            String streamName = "AI_USAGE_STREAM";
            String durableName = "usage-persistence-worker";

            // 1. Ensure JetStream Stream exists
            JetStreamManagement jsm = natsConnection.jetStreamManagement();
            try {
                jsm.getStreamInfo(streamName);
            } catch (Exception e) {
                log.info("[UsageWorker] Creating Stream: {}", streamName);
                jsm.addStream(StreamConfiguration.builder()
                        .name(streamName)
                        .subjects(usageSubject)
                        .storageType(StorageType.File)
                        .build());
            }

            // 2. Take StreamContext
            StreamContext streamContext = natsConnection.getStreamContext(streamName);

            // 3. Configure Consumer (Durable Pull)
            ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
                    .durable(durableName)
                    .filterSubject(usageSubject)
                    .ackPolicy(AckPolicy.Explicit)
                    .maxDeliver(3)
                    .ackWait(Duration.ofSeconds(30))
                    .build();
            
            ConsumerContext consumerContext = streamContext.createOrUpdateConsumer(consumerConfig);

            // 4. Start Consume
            consumerContext.consume(msg -> {
                try {
                    UsageEvent event = objectMapper.readValue(msg.getData(), UsageEvent.class);
                    processUsageEvent(event, msg);
                } catch (Exception e) {
                    log.error("[UsageWorker] Failed to parse UsageEvent. terminating message.", e);
                    msg.term();
                }
            });

            log.info("[UsageWorker] Consumer is now running...");

        } catch (Exception e) {
            log.error("[UsageWorker] Critical error starting NATS listener", e);
        }
    }

    private void processUsageEvent(UsageEvent event, Message natsMsg) {
        log.info("[UsageRecord] User: {}, Session: {}, Model: {}, Tokens: [P:{}, C:{}, T:{}]", 
                event.userId(), event.sessionId(), event.model(), 
                event.promptTokens(), event.completionTokens(), event.totalTokens());
        
        // TODO: Persist to DB for cost calculation/billing
        
        
        natsMsg.ack();
    }
}
