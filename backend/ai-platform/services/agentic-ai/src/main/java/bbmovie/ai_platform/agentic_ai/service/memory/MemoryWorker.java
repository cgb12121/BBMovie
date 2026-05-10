package bbmovie.ai_platform.agentic_ai.service.memory;

import bbmovie.ai_platform.agentic_ai.entity.ChatMessage;
import bbmovie.ai_platform.agentic_ai.entity.Sender;
import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;

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

    @EventListener(ApplicationReadyEvent.class)
    public void startListening() {
        log.info("[Worker] Memory Worker active on subject: {}", memorySubject);
        Dispatcher dispatcher = natsConnection.createDispatcher(msg -> {
            try {
                MemoryEvent event = objectMapper.readValue(msg.getData(), MemoryEvent.class);
                processEvent(event);
            } catch (Exception e) {
                log.error("[Worker] Event processing failed", e);
            }
        });
        dispatcher.subscribe(memorySubject, memoryQueue);
    }

    private void processEvent(MemoryEvent event) {
        switch (event.type()) {
            case ADD -> handleAdd(event);
            case CLEAR -> handleClear(event);
            default -> log.warn("[Worker] Unknown event type: {}", event.type());
        }
    }

    private void handleAdd(MemoryEvent event) {
        log.info("[Worker] Saving messages for Session: {}, User: {}", event.sessionId(), event.userId());
        UUID sessionId = UUID.fromString(event.sessionId());
        UUID userId = UUID.fromString(event.userId());
            
        List<ChatMessage> entities = event.jsonMessages().stream()
                .map(this::deserializeMessage)
                .map((Message msg) -> ChatMessage.builder()
                        .id(UUID.randomUUID())
                        .sessionId(sessionId)
                        .userId(userId)
                        .content(msg.getText())
                        .senderType(mapSenderType(msg))
                        .createdAt(Instant.now())
                        .build()
                        .asNew()
                )
                .collect(Collectors.toList());

        messageRepository.saveAll(entities)
                .doOnComplete(() -> log.info("[Worker] DB Sync Complete: {} messages", entities.size()))
                .subscribe();
    }

    private void handleClear(MemoryEvent event) {
        log.info("[Worker] Clearing history for Session: {}", event.sessionId());
        messageRepository.deleteAllBySessionId(UUID.fromString(event.sessionId()))
                .doOnSuccess(v -> log.info("[Worker] History cleared for session: {}", event.sessionId()))
                .subscribe();
    }

    private Message deserializeMessage(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            String type = node.path("messageType").asString("USER");
            return switch (type) {
                case "USER" -> objectMapper.treeToValue(node, UserMessage.class);
                case "ASSISTANT" -> objectMapper.treeToValue(node, AssistantMessage.class);
                case "SYSTEM" -> objectMapper.treeToValue(node, SystemMessage.class);
                default -> objectMapper.treeToValue(node, UserMessage.class);
            };
        } catch (Exception e) {
            return new UserMessage("Error");
        }
    }

    private Sender mapSenderType(Message message) {
        return switch (message.getMessageType()) {
            case USER -> Sender.USER;
            case ASSISTANT -> Sender.AGENT;
            case SYSTEM -> Sender.SYSTEM;
            default -> Sender.USER;
        };
    }
}
