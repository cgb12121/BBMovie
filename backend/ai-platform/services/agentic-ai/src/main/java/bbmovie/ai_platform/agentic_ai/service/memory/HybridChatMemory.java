package bbmovie.ai_platform.agentic_ai.service.memory;

import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.agentic_ai.utils.AiMessageUtils;
import io.nats.client.Connection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class HybridChatMemory implements ChatMemory {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final Connection natsConnection;

    @Value("${nats.memory.subject:ai.memory.sync}")
    private String memorySubject;

    private static final int MESSAGE_FETCH_LIMIT = 20;
    private static final String REDIS_CHAT_MEMORY_PREFIX = "chat:memory:";
    private static final Duration REDIS_TTL = Duration.ofHours(1);

    @Override
    public void add(String conversationId, List<Message> messages) {
        // Parse composite ID: "userId:sessionId"
        String[] ids = conversationId.split(":");
        String userId =  ids[0];
        String sessionId = ids[1];

        // 1. Save to Redis (Short-term)
        String redisKey = REDIS_CHAT_MEMORY_PREFIX + sessionId;
        log.debug("[Memory] Syncing {} messages for Session: {}, User: {}", messages.size(), sessionId, userId);

        try {
            // 1. Serialize once for both Redis and NATS
            List<String> jsonMessages = messages.stream()
                    .map(m -> {
                        try {
                            return objectMapper.writeValueAsString(m);
                        } catch (Exception e) {
                            log.error("Serialization failed: {}", e.getMessage());
                            throw new RuntimeException("Serialization failed");
                        }
                    })
                    .toList();

            // 2. Save to Redis (Short-term) + Trim to keep window size
            Mono.fromCallable(() ->
                        redisTemplate.opsForList()
                            .rightPushAll(redisKey, jsonMessages)
                            .then(redisTemplate.opsForList().trim(redisKey, -MESSAGE_FETCH_LIMIT, -1))
                            .then(redisTemplate.expire(redisKey, REDIS_TTL))
                            .block(Duration.ofSeconds(2))
                    )
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();

            // 3. Send Event via NATS (Long-term)
            MemoryEvent event = MemoryEvent.add(sessionId, userId, jsonMessages);
            natsConnection.publish(memorySubject, objectMapper.writeValueAsBytes(event));
            
        } catch (Exception e) {
            log.error("[Memory] Memory sync failed: {}", e.getMessage());
        }
    }

    @Override
    public List<Message> get(String conversationId) {
        String[] ids = conversationId.split(":");
        if (ids.length < 2) {
            log.warn("[Memory] Invalid conversationId format: {}. Expected userId:sessionId", conversationId);
            return List.of();
        }
        
        String userId = ids[0];
        String sessionId = ids[1];
        String key = REDIS_CHAT_MEMORY_PREFIX + sessionId;
        
        log.debug("[Memory] Retrieving history for Session: {}, User: {}", sessionId, userId);

        // 1. Try to fetch from redis (Safe Blocking)
        List<String> jsonMessages = redisTemplate.opsForList().range(key, -MESSAGE_FETCH_LIMIT, -1)
                .collectList()
                .block(Duration.ofSeconds(2));

        if (jsonMessages != null && !jsonMessages.isEmpty()) {
            log.debug("[Memory] Redis HIT for session: {}", sessionId);
            return jsonMessages.stream()
                    .map(msg -> AiMessageUtils.deserializeMessage(msg))
                    .collect(Collectors.toList());
        }

        log.debug("[Memory] Redis miss for {}, falling back to DB", sessionId);
        return messageRepository.findAllBySessionIdOrderByCreatedAtAsc(UUID.fromString(sessionId))
                .takeLast(MESSAGE_FETCH_LIMIT)
                .map(msg -> AiMessageUtils.mapToSpringAiMessage(msg))
                .collectList()
                .block(Duration.ofSeconds(5));
    }

    @Override
    public void clear(String conversationId) {
        String[] ids = conversationId.split(":");
        String userId = ids.length > 1 ? ids[0] : "unknown";
        String sessionId = ids.length > 1 ? ids[1] : ids[0];

        String key = REDIS_CHAT_MEMORY_PREFIX + sessionId;
        redisTemplate.delete(key).subscribe();

        // Send event CLEAR via NATS
        try {
            MemoryEvent event = MemoryEvent.clear(sessionId, userId);
            natsConnection.publish(memorySubject, objectMapper.writeValueAsBytes(event));
        } catch (Exception e) {
            log.error("[Memory] Failed to publish CLEAR event", e);
        }
    }
}
