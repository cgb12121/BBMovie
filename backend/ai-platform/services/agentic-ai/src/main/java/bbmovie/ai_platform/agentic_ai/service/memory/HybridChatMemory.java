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
import java.util.Collections;
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

        // NOTE: Spring AI's ChatMemory interface is synchronous (void add()).
        // This means this method is invoked by the MessageChatMemoryAdvisor on the event loop thread
        // during an active request. To avoid blocking Netty's I/O threads directly, we offload all
        // work to a boundedElastic thread via subscribeOn(), then call block() to re-synchronize.
        //
        // The block() call is safe ONLY because it runs on a boundedElastic worker thread,
        // never on a Netty event loop thread. If this constraint is ever violated (e.g., by
        // calling add() from a reactor context that lacks a scheduler), it will throw
        // BlockingOperationNotAllowedException at runtime.
        //
        // A proper fix would require Spring AI to expose a reactive ChatMemory contract.
        // Tracked as: https://github.com/spring-projects/spring-ai/issues (reactive ChatMemory)
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

                try {
                    // 2. Synchronous save to Redis (Short-term) for immediate consistency.
                    // Wrapped in try-catch: if Redis is unreachable, we degrade gracefully
                    // and rely solely on NATS → DB for persistence (step 3).
                    redisTemplate.opsForList()
                            .rightPushAll(redisKey, jsonMessages)
                            .then(redisTemplate.opsForList().trim(redisKey, -MESSAGE_FETCH_LIMIT, -1))
                            .then(redisTemplate.expire(redisKey, REDIS_TTL))
                            .block(Duration.ofSeconds(2));
                } catch (Exception redisEx) {
                    log.warn("[Memory] Redis write failed for session {}, degrading to NATS-only: {}",
                            convId.sessionId(), redisEx.getMessage());
                    // Intentional fall-through: NATS publish below still ensures DB persistence.
                }

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
            // 1. Try to fetch from Redis (fast path)
            // Wrapped in try-catch: if Redis is unreachable, fall through to DB immediately.
            try {
                List<String> jsonMessages = redisTemplate.opsForList().range(key, -MESSAGE_FETCH_LIMIT, -1)
                        .collectList()
                        .block(Duration.ofSeconds(2));

                if (jsonMessages != null && !jsonMessages.isEmpty()) {
                    log.debug("[Memory] Redis HIT for session: {}", convId.sessionId());
                    return jsonMessages.stream()
                            .map(AiMessageUtils::deserializeMessage)
                            .collect(Collectors.toList());
                }
            } catch (Exception redisEx) {
                log.warn("[Memory] Redis read failed for session {}, falling back to DB: {}",
                        convId.sessionId(), redisEx.getMessage());
                // Intentional fall-through to DB query below.
            }

            log.debug("[Memory] Redis miss for {}, falling back to DB", convId.sessionId());
            // Fetch only the last N messages at the DB level (LIMIT in SQL).
            // DB returns newest-first (DESC); we reverse in memory to restore chronological
            // order (ASC) for the AI context window. Reversing N=20 items is O(1) effectively.
            return messageRepository.findLastNBySessionId(convId.sessionId(), MESSAGE_FETCH_LIMIT)
                    .map(AiMessageUtils::mapToSpringAiMessage)
                    .collectList()
                    .map(list -> { 
                        Collections.reverse(list); 
                        return list; 
                    })
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
