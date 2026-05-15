package bbmovie.ai_platform.agentic_ai.service.memory;

import bbmovie.ai_platform.agentic_ai.entity.ChatMessage;
import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.agentic_ai.utils.AiMessageUtils;
import io.nats.client.*;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.ReplayPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * MemoryWorker is a background service that persists conversation history to the database.
 * 
 * It acts as a NATS JetStream consumer, pulling memory events asynchronously.
 * Key features:
 * - JetStream Pull Consumer: Reliable message processing with a durable consumer.
 * - Reactive Flow: Fully non-blocking processing using Project Reactor.
 * - Redis Deduplication: Uses a "Set If Not Absent" strategy in Redis to prevent duplicate messages.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryWorker {

    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final Connection natsConnection;
    @Qualifier("rRedisTemplate") private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Value("${nats.memory.subject:ai.memory.sync}")
    private String memorySubject;

    @Value("${nats.memory.queue:memory-workers}")
    private String memoryQueue;

    private static final String DEDUP_KEY_PREFIX = "dedup:msg:";
    private static final Duration DEDUP_TTL = Duration.ofMinutes(5);

    /**
     * Use Simplified from JetStream API (Pull Consumer)
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        log.info("[Worker] Initializing Simplified JetStream Pull Worker on subject: {}", memorySubject);
        try {
            String streamName = "AI_MEMORY_STREAM";
            String durableName = "memory-persistence-worker";

            // 1. Ensure Jet Stream exist
            JetStreamManagement jsm = natsConnection.jetStreamManagement();
            try {
                jsm.getStreamInfo(streamName);
            } catch (Exception e) {
                log.info("[Worker] Creating Stream: {}", streamName);
                jsm.addStream(StreamConfiguration.builder()
                        .name(streamName)
                        .subjects(memorySubject)
                        .storageType(StorageType.File)
                        .build());
            }

            // 2. Take StreamContext
            StreamContext streamContext = natsConnection.getStreamContext(streamName);

            // 3. Configure Consumer (Durable Pull)
            // Enhanced with production-grade reliability settings
            ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
                    .durable(durableName)
                    .filterSubject(memorySubject)
                    .ackPolicy(AckPolicy.Explicit)
                    .maxDeliver(5)
                    .ackWait(Duration.ofSeconds(30))
                    .replayPolicy(ReplayPolicy.Instant)
                    .build();
            
            ConsumerContext consumerContext = streamContext.createOrUpdateConsumer(consumerConfig);

            // 4. Start Consume (Support Async Pull)
            // Ack is handled manually in handleAdd/handleClear
            consumerContext.consume(msg -> {
                try {
                    MemoryEvent event = objectMapper.readValue(msg.getData(), MemoryEvent.class);
                    processEvent(event, msg);
                } catch (Exception e) {
                    log.error("[Worker] Failed to parse MemoryEvent. Message will be terminated (poison message).", e);
                    msg.term(); // Permanent failure, don't retry
                }
            });

            log.info("[Worker] Pull Consumer is now running...");

        } catch (Exception e) {
            log.error("[Worker] Critical error starting NATS Pull listener", e);
        }
    }

    private void processEvent(MemoryEvent event, io.nats.client.Message natsMsg) {
        switch (event.type()) {
            case ADD -> handleAdd(event, natsMsg);
            case CLEAR -> handleClear(event, natsMsg);
            default -> {
                log.warn("[Worker] Unknown event type: {}", event.type());
                natsMsg.ack(); 
            }
        }
    }

    private void handleAdd(MemoryEvent event, io.nats.client.Message natsMsg) {
        log.info("[Worker] Processing ADD event for Session: {}", event.sessionId());
        
        UUID sessionId;
        UUID userId;
        try {
            sessionId = UUID.fromString(event.sessionId());
            userId = UUID.fromString(event.userId());
        } catch (IllegalArgumentException e) {
            log.error("[Worker] Invalid UUID in event: {}", e.getMessage());
            natsMsg.term();
            return;
        }

        Instant now = event.timestamp();

        Flux.fromIterable(event.jsonMessages())
                .map(AiMessageUtils::deserializeMessage)
                .flatMap(msg -> isDuplicate(sessionId, msg)
                        .flatMap(isDup -> {
                            if (isDup) {
                                log.debug("[Worker] Duplicate detected for session {}, skipping message.", sessionId);
                                return Mono.empty();
                            }
                            return Mono.just(mapToEntity(msg, sessionId, userId, now));
                        })
                )
                .collectList()
                .flatMap(entities -> {
                    if (entities.isEmpty()) {
                        log.info("[Worker] All messages in event are duplicates or empty. Skipping.");
                        return Mono.empty();
                    }
                    return messageRepository.saveAll(entities).then();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> {
                    log.info("[Worker] DB Sync Complete for session: {}", sessionId);
                    natsMsg.ack();
                })
                .doOnError(ex -> {
                    log.error("[Worker] DB Sync Failed for session: {}", sessionId, ex);
                    natsMsg.nak();
                })
                .subscribe();
    }

    /**
     * Checks if the message is a duplicate using Redis SET NX.
     */
    private Mono<Boolean> isDuplicate(UUID sessionId, Message msg) {
        try {
            String content = msg.getText();
            String sender = msg.getMessageType().name();
            
            // Create a hash of the content to use in the Redis key
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            String hashStr = HexFormat.of().formatHex(hash);
            
            String dedupKey = DEDUP_KEY_PREFIX + sessionId + ":" + hashStr + ":" + sender;
            
            return redisTemplate.opsForValue()
                    .setIfAbsent(dedupKey, "1", DEDUP_TTL)
                    .map(success -> !success) // If setIfAbsent returns false, it means key already exists (it IS a duplicate)
                    .defaultIfEmpty(false);
        } catch (Exception e) {
            log.error("[Worker] Deduplication check failed", e);
            return Mono.just(false); // Fail-open: allow saving if dedup check fails
        }
    }

    /**
     * Maps a Spring AI Message to a ChatMessage entity.
     */
    private ChatMessage mapToEntity(Message msg, UUID sessionId, UUID userId, Instant createdAt) {
        String thinking = null;
        Long promptTokens = null;
        Long completionTokens = null;
        
        if (msg instanceof AssistantMessage assistantMessage) {
            Object think = assistantMessage.getMetadata().get("think");
            if (think != null) {
                thinking = think.toString();
            }
            
            Object pTokens = assistantMessage.getMetadata().get("prompt_tokens");
            if (pTokens != null) {
                promptTokens = Long.valueOf(pTokens.toString());
            }
            
            Object cTokens = assistantMessage.getMetadata().get("completion_tokens");
            if (cTokens != null) {
                completionTokens = Long.valueOf(cTokens.toString());
            }
        }

        return ChatMessage.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .userId(userId)
                .content(msg.getText())
                .thinking(thinking)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .senderType(AiMessageUtils.mapSenderType(msg))
                .createdAt(createdAt)
                .build()
                .asNew();
    }

    private void handleClear(MemoryEvent event, io.nats.client.Message natsMsg) {
        log.info("[Worker] Clearing history for Session: {}", event.sessionId());
        messageRepository.deleteAllBySessionId(UUID.fromString(event.sessionId()))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSuccess(v -> {
                    log.info("[Worker] History cleared for session: {}", event.sessionId());
                    natsMsg.ack();
                })
                .doOnError(e -> {
                    log.error("[Worker] Failed to clear session: {}", event.sessionId(), e);
                    natsMsg.nak();
                })
                .subscribe();
    }
}
