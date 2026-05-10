package bbmovie.ai_platform.agentic_ai.service.memory;

import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import io.nats.client.Connection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class HybridChatMemory implements ChatMemory {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final Connection natsConnection;

    @Value("${nats.memory.subject:ai.memory.sync}")
    private String memorySubject;

    private static final int MESSAGE_FETCH_LIMIT = 20;
    private static final String REDIS_PREFIX = "chat:memory:";
    private static final Duration REDIS_TTL = Duration.ofHours(1);

    public HybridChatMemory(
            ReactiveRedisTemplate<String, String> redisTemplate, 
            MessageRepository messageRepository,
            ObjectMapper objectMapper,
            Connection natsConnection) {
        this.redisTemplate = redisTemplate;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
        this.natsConnection = natsConnection;
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        // Parse composite ID: "userId:sessionId"
        String[] ids = conversationId.split(":");
        String userId = ids.length > 1 ? ids[0] : "unknown";
        String sessionId = ids.length > 1 ? ids[1] : ids[0];

        // 1. Lưu vào Redis (Short-term)
        String redisKey = REDIS_PREFIX + sessionId;
        log.info("[Memory] Syncing to Redis for Session: {}, User: {}", sessionId, userId);
        
        for (Message message : messages) {
            try {
                String json = objectMapper.writeValueAsString(message);
                redisTemplate.opsForList().rightPush(redisKey, json)
                        .publishOn(Schedulers.boundedElastic())
                        .block(Duration.ofSeconds(2)); 
            } catch (Exception e) {
                log.error("[Memory] Redis push failed: {}", e.getMessage());
            }
        }
        redisTemplate.expire(redisKey, REDIS_TTL).subscribe();

        // 2. Bắn Event qua NATS (Long-term)
        try {
            List<String> jsonMsgs = new ArrayList<>();
            for (Message m : messages) {
                jsonMsgs.add(objectMapper.writeValueAsString(m));
            }
            MemoryEvent event = MemoryEvent.add(sessionId, userId, jsonMsgs);
            natsConnection.publish(memorySubject, objectMapper.writeValueAsBytes(event));
        } catch (Exception e) {
            log.error("[Memory] NATS publish failed", e);
        }
    }

    @Override
    public List<Message> get(String sessionId) {
        String key = REDIS_PREFIX + sessionId;
        
        // Thử lấy từ Redis trước
        List<String> jsonMessages = redisTemplate.opsForList().range(key, -MESSAGE_FETCH_LIMIT, -1)
                .collectList()
                .block(Duration.ofSeconds(2));

        if (jsonMessages != null && !jsonMessages.isEmpty()) {
            return jsonMessages.stream()
                    .map(this::deserializeMessage)
                    .collect(Collectors.toList());
        }

        log.warn("[Memory] Redis miss for {}, falling back to DB", sessionId);
        return messageRepository.findAllBySessionIdOrderByCreatedAtAsc(UUID.fromString(sessionId))
                .takeLast(MESSAGE_FETCH_LIMIT)
                .map(this::mapToSpringAiMessage)
                .collectList()
                .block(Duration.ofSeconds(5));
    }

    @Override
    public void clear(String conversationId) {
        String[] ids = conversationId.split(":");
        String userId = ids.length > 1 ? ids[0] : "unknown";
        String sessionId = ids.length > 1 ? ids[1] : ids[0];

        String key = REDIS_PREFIX + sessionId;
        redisTemplate.delete(key).subscribe();

        // Bắn event CLEAR qua NATS
        try {
            MemoryEvent event = MemoryEvent.clear(sessionId, userId);
            natsConnection.publish(memorySubject, objectMapper.writeValueAsBytes(event));
        } catch (Exception e) {
            log.error("[Memory] Failed to publish CLEAR event", e);
        }
    }

    private Message mapToSpringAiMessage(bbmovie.ai_platform.agentic_ai.entity.ChatMessage entity) {
        return switch (entity.getSenderType()) {
            case USER -> new UserMessage(entity.getContent());
            case AGENT -> new AssistantMessage(entity.getContent());
            case SYSTEM -> new SystemMessage(entity.getContent());
            default -> new UserMessage(entity.getContent());
        };
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
            log.error("Deserialization failed", e);
            return new UserMessage("Error");
        }
    }
}
