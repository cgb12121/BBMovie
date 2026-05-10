package bbmovie.ai_platform.agentic_ai.service.memory;

import bbmovie.ai_platform.agentic_ai.entity.ChatMessage;
import bbmovie.ai_platform.agentic_ai.entity.Sender;
import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class HybridChatMemory implements ChatMemory {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    private static final String REDIS_PREFIX = "chat:memory:";
    private static final Duration REDIS_TTL = Duration.ofHours(1);

    public HybridChatMemory(
            ReactiveRedisTemplate<String, String> redisTemplate, 
            MessageRepository messageRepository,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.messageRepository = messageRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(String sessionId, List<Message> messages) {
        // 1. Add to Redis (Short-term)
        String key = REDIS_PREFIX + sessionId;
        log.info("[Memory] Adding {} messages to Redis with key: {}", messages.size(), key);
        for (Message message : messages) {
            try {
                String json = objectMapper.writeValueAsString(message);
                redisTemplate.opsForList().rightPush(key, json)
                        .doOnSuccess(v -> log.debug("[Memory] Message pushed to Redis"))
                        .doOnError(e -> log.error("[Memory] Redis push failed", e))
                        .subscribe();
            } catch (Exception e) {
                log.error("[Memory] Failed to serialize message for Redis", e);
            }
        }
        redisTemplate.expire(key, REDIS_TTL).subscribe();

        // 2. Sync to DB (Long-term) in background
        try {
            UUID sessionUuid = UUID.fromString(sessionId);
            List<ChatMessage> entities = messages.stream()
                    .map(message -> ChatMessage.builder()
                            .id(UUID.randomUUID())
                            .sessionId(sessionUuid)
                            .content(message.getText())
                            .senderType(mapSenderType(message))
                            .createdAt(Instant.now())
                            .build()
                            .asNew())
                    .collect(Collectors.toList());

            log.info("[Memory] Syncing {} messages to DB for session: {}", entities.size(), sessionId);
            messageRepository.saveAll(entities)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe(
                            saved -> log.info("[Memory] Successfully synced message to DB: {}", saved.getId()),
                            error -> log.error("[Memory] Failed to sync messages to DB", error)
                    );
        } catch (IllegalArgumentException e) {
            log.error("Invalid session ID format: {}", sessionId);
        }
    }

    @Override
    public List<Message> get(String sessionId) {
        // 1. Try Redis
        String key = REDIS_PREFIX + sessionId;
        log.info("[Memory] Fetching from Redis with key: {}", key);
        List<String> jsons = redisTemplate.opsForList().range(key, 0, -1) // Get all
                .collectList()
                .block();
        
        if (jsons != null && !jsons.isEmpty()) {
            return jsons.stream()
                    .map(this::deserializeMessage)
                    .collect(Collectors.toList());
        }

        // 2. Load from DB if Redis is empty
        log.info("Redis cache miss for session {}, loading from DB...", sessionId);
        try {
            UUID sessionUuid = UUID.fromString(sessionId);
            
            // For now, fetch last 100 messages as a default "context window" 
            // since we don't have lastN from the interface anymore
            List<ChatMessage> entities = messageRepository.findAllBySessionIdOrderByCreatedAtDesc(sessionUuid)
                    .take(100)
                    .collectList()
                    .block();

            if (entities == null || entities.isEmpty()) {
                return Collections.emptyList();
            }

            Collections.reverse(entities);

            List<Message> loadedMessages = entities.stream()
                    .map(this::mapToSpringAiMessage)
                    .collect(Collectors.toList());

            // Populate Redis
            loadedMessages.forEach(msg -> {
                try {
                    String json = objectMapper.writeValueAsString(msg);
                    redisTemplate.opsForList().rightPush(key, json).subscribe();
                } catch (Exception e) {}
            });
            redisTemplate.expire(key, REDIS_TTL).subscribe();

            return loadedMessages;
        } catch (IllegalArgumentException e) {
            log.error("Invalid session ID format: {}", sessionId);
            return Collections.emptyList();
        }
    }

    @Override
    public void clear(String sessionId) {
        redisTemplate.delete(REDIS_PREFIX + sessionId).subscribe();
    }

    private Sender mapSenderType(Message message) {
        return switch (message.getMessageType()) {
            case USER -> Sender.USER;
            case ASSISTANT -> Sender.AGENT;
            case SYSTEM -> Sender.SYSTEM;
            default -> Sender.USER;
        };
    }

    private Message mapToSpringAiMessage(ChatMessage entity) {
        return switch (entity.getSenderType()) {
            case USER -> new UserMessage(entity.getContent());
            case AGENT -> new AssistantMessage(entity.getContent());
            case SYSTEM -> new SystemMessage(entity.getContent());
            default -> new UserMessage(entity.getContent());
        };
    }

    private Message deserializeMessage(String json) {
        try {
            // This is a bit tricky as Message is an interface and might need a custom deserializer
            // For now, we'll assume basic User/Assistant messages
            // ideally we'd use a more robust polymorphic deserializer
            if (json.contains("\"messageType\":\"USER\"")) return objectMapper.readValue(json, UserMessage.class);
            if (json.contains("\"messageType\":\"ASSISTANT\"")) return objectMapper.readValue(json, AssistantMessage.class);
            if (json.contains("\"messageType\":\"SYSTEM\"")) return objectMapper.readValue(json, SystemMessage.class);
            return objectMapper.readValue(json, UserMessage.class);
        } catch (Exception e) {
            log.error("Failed to deserialize message from Redis", e);
            return new UserMessage("Error loading message");
        }
    }
}
