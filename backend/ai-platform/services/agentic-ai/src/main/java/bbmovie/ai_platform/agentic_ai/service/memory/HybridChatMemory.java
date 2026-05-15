package bbmovie.ai_platform.agentic_ai.service.memory;
import bbmovie.ai_platform.agentic_ai.dto.ConversationId;
import bbmovie.ai_platform.agentic_ai.repository.MessageRepository;
import bbmovie.ai_platform.agentic_ai.utils.AiMessageUtils;
import io.nats.client.JetStream;
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
import java.util.stream.Collectors;

/**
 * HybridChatMemory provides a dual-layer persistence strategy for conversation history.
 * 
 * 1. L1 (Short-term): Synchronous save to Redis for ultra-fast retrieval in the next request.
 * 2. L2 (Long-term): Asynchronous event broadcast via NATS. A background worker (MemoryWorker) 
 *    listens to these events and persists the messages to the primary SQL database.
 * 
 * This architecture ensures the Chat flow is never blocked by slow database I/O.
 */
@Slf4j
@RequiredArgsConstructor
public class HybridChatMemory implements ChatMemory {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MessageRepository messageRepository;
    private final ObjectMapper objectMapper;
    private final JetStream jetStream;

    @Value("${nats.memory.subject:ai.memory.sync}")
    private String memorySubject;

    private static final int MESSAGE_FETCH_LIMIT = 20;
    private static final String REDIS_CHAT_MEMORY_PREFIX = "chat:memory:";
    private static final Duration REDIS_TTL = Duration.ofHours(1);

    @Override
    public void add(String conversationId, List<Message> messages) {
        ConversationId convId;
        try {
            convId = ConversationId.of(conversationId);
        } catch (IllegalArgumentException e) {
            log.error("[Memory] Failed to parse conversationId: {}", conversationId);
            return;
        }

        String redisKey = REDIS_CHAT_MEMORY_PREFIX + convId.sessionId();
        log.debug("[Memory] Syncing {} messages for Session: {}", messages.size(), convId.sessionId());

        Mono.fromRunnable(() -> {
            try {
                // 1. Serialize once
                List<String> jsonMessages = messages.stream()
                        .map(m -> {
                            try {
                                return objectMapper.writeValueAsString(m);
                            } catch (Exception e) {
                                throw new RuntimeException("Serialization failed", e);
                            }
                        })
                        .toList();

                // 2. Synchronous save to Redis (Short-term) for immediate consistency
                // We use block() here inside boundedElastic() to ensure the next request can see these messages.
                redisTemplate.opsForList()
                        .rightPushAll(redisKey, jsonMessages)
                        .then(redisTemplate.opsForList().trim(redisKey, -MESSAGE_FETCH_LIMIT, -1))
                        .then(redisTemplate.expire(redisKey, REDIS_TTL))
                        .block(Duration.ofSeconds(2));

                // 3. Send Event via NATS JetStream (Long-term)
                // Use JetStream for at-least-once delivery guarantees.
                MemoryEvent event = MemoryEvent.add(convId.sessionId().toString(), convId.userId().toString(), jsonMessages);
                jetStream.publishAsync(memorySubject, objectMapper.writeValueAsBytes(event));
                
            } catch (Exception e) {
                log.error("[Memory] Memory sync failed for session {}: {}", convId.sessionId(), e.getMessage());
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .block(Duration.ofSeconds(5)); // Safe blocking of the calling thread (not event loop)
    }

    @Override
    public List<Message> get(String conversationId) {
        ConversationId convId;
        try {
            convId = ConversationId.of(conversationId);
        } catch (Exception e) {
            return List.of();
        }
        
        String key = REDIS_CHAT_MEMORY_PREFIX + convId.sessionId();
        
        return Mono.fromCallable(() -> {
            // 1. Try to fetch from redis
            List<String> jsonMessages = redisTemplate.opsForList().range(key, -MESSAGE_FETCH_LIMIT, -1)
                    .collectList()
                    .block(Duration.ofSeconds(2));

            if (jsonMessages != null && !jsonMessages.isEmpty()) {
                log.debug("[Memory] Redis HIT for session: {}", convId.sessionId());
                return jsonMessages.stream()
                        .map(AiMessageUtils::deserializeMessage)
                        .collect(Collectors.toList());
            }

            log.debug("[Memory] Redis miss for {}, falling back to DB", convId.sessionId());
            return messageRepository.findAllBySessionIdOrderByCreatedAtAsc(convId.sessionId())
                    .takeLast(MESSAGE_FETCH_LIMIT)
                    .map(AiMessageUtils::mapToSpringAiMessage)
                    .collectList()
                    .block(Duration.ofSeconds(5));
        })
        .subscribeOn(Schedulers.boundedElastic())
        .block(Duration.ofSeconds(10));
    }

    @Override
    public void clear(String conversationId) {
        ConversationId convId;
        try {
            convId = ConversationId.of(conversationId);
        } catch (Exception e) {
            log.error("[Memory] Failed to parse conversationId for clear: {}", conversationId);
            return;
        }

        String key = REDIS_CHAT_MEMORY_PREFIX + convId.sessionId();
        
        Mono.fromRunnable(() -> {
            try {
                redisTemplate.delete(key).block(Duration.ofSeconds(2));
                MemoryEvent event = MemoryEvent.clear(convId.sessionId().toString(), convId.userId().toString());
                jetStream.publishAsync(memorySubject, objectMapper.writeValueAsBytes(event));
            } catch (Exception e) {
                log.error("[Memory] Failed to clear session: {}", convId.sessionId(), e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(); // Non-blocking clear is fine as it's not critical for the next request usually
    }
}
