package bbmovie.ai_platform.agentic_ai.service.memory;

import bbmovie.ai_platform.agentic_ai.entity.ChatMessage;
import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.agentic_ai.utils.AiMessageUtils;
import io.nats.client.*;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MemoryWorker {

    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final Connection natsConnection;

    @Value("${nats.memory.subject:ai.memory.sync}")
    private String memorySubject;

    @Value("${nats.memory.queue:memory-workers}")
    private String memoryQueue;

    /**
     * Sử dụng Simplified JetStream API (Pull Consumer) từ jnats 2.25.3.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        log.info("[Worker] Initializing Simplified JetStream Pull Worker on subject: {}", memorySubject);
        try {
            String streamName = "AI_MEMORY_STREAM";
            String durableName = "memory-persistence-worker";

            // 1. Đảm bảo Stream tồn tại
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

            // 2. Lấy StreamContext
            StreamContext streamContext = natsConnection.getStreamContext(streamName);

            // 3. Cấu hình Consumer (Durable Pull)
            ConsumerConfiguration cc = ConsumerConfiguration.builder()
                    .durable(durableName)
                    .filterSubject(memorySubject)
                    .build();
            
            ConsumerContext consumerContext = streamContext.createOrUpdateConsumer(cc);

            // 4. Bắt đầu Consume (Hỗ trợ Async Pull)
            consumerContext.consume(msg -> {
                try {
                    MemoryEvent event = objectMapper.readValue(msg.getData(), MemoryEvent.class);
                    processEvent(event, msg);
                } catch (Exception e) {
                    log.error("[Worker] Failed to parse MemoryEvent", e);
                    msg.term(); // Hủy message lỗi để không bị loop
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
        log.info("[Worker] Saving messages for Session: {}, User: {}", event.sessionId(), event.userId());
        UUID sessionId = UUID.fromString(event.sessionId());
        UUID userId = UUID.fromString(event.userId());
        Instant now = event.timestamp();
            
        List<ChatMessage> entities = event.jsonMessages().stream()
                .map(msg -> AiMessageUtils.deserializeMessage(msg))
                .filter(msg -> {
                    // Tránh lưu duplicate nếu tin nhắn vừa mới được lưu trong vòng 5s gần đây
                    Boolean exists = messageRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                            .filter(existing -> existing.getContent().equals(msg.getText()) && 
                                               existing.getSenderType() == AiMessageUtils.mapSenderType(msg))
                            .any(existing -> Duration.between(existing.getCreatedAt(), now).abs().getSeconds() < 5)
                            .block(Duration.ofSeconds(2));
                    return exists == null || !exists;
                })
                .map((Message msg) -> ChatMessage.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .userId(userId)
                        .content(msg.getText())
                        .senderType(AiMessageUtils.mapSenderType(msg))
                        .createdAt(now)
                        .build()
                        .asNew()
                )
                .collect(Collectors.toList());

        if (entities.isEmpty()) {
            log.info("[Worker] All messages in event are already persisted. Skipping.");
            natsMsg.ack();
            return;
        }

        messageRepository.saveAll(entities)
                .then()
                .doOnSuccess(v -> {
                    log.info("DB Sync Complete");
                    natsMsg.ack();
                })
                .doOnError(ex -> {
                    log.error("DB Sync Failed", ex);
                    natsMsg.nak();
                })
                .subscribe();
    }

    private void handleClear(MemoryEvent event, io.nats.client.Message natsMsg) {
        log.info("[Worker] Clearing history for Session: {}", event.sessionId());
        messageRepository.deleteAllBySessionId(UUID.fromString(event.sessionId()))
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
