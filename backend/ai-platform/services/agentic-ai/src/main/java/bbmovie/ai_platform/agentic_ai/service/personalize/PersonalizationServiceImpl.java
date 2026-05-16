package bbmovie.ai_platform.agentic_ai.service.personalize;

import bbmovie.ai_platform.agentic_ai.dto.MemoryRecord;
import bbmovie.ai_platform.agentic_ai.dto.request.UpdateAgentConfigDto;
import bbmovie.ai_platform.agentic_ai.entity.AgentPersonalization;
import bbmovie.ai_platform.agentic_ai.repository.MemoryRepository;
import bbmovie.ai_platform.agentic_ai.repository.PersonalizationRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * PersonalizationServiceImpl manages user-specific context and behavioral history to tailor AI responses.
 *
 * <p>It employs a triple-layer strategy for efficient context retrieval:
 * <ol>
 *   <li><b>L1 (In-memory, Caffeine)</b>: keyed by {@code userId}. Max 500 users, 30-min access TTL.
 *       Evicted precisely via {@link #invalidateCache(UUID)} on any write.</li>
 *   <li><b>L2 (Distributed, Redis)</b>: keyed by {@code "personalization:{userId}"}. TTL: 24 hours.
 *       Shared across service instances.</li>
 *   <li><b>L3 (Cold storage)</b>: Merges explicit instructions from PostgreSQL with semantic
 *       behavioral facts from Qdrant (vector similarity search).</li>
 * </ol>
 *
 * <p><b>Two separate memory stores:</b>
 * <ul>
 *   <li>{@code agent_memory} (SQL) — user-visible, capped at 20 records, manageable by the user.</li>
 *   <li>Qdrant (Vector DB) — AI's long-term semantic index, uncapped, never deleted via this service.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizationServiceImpl implements PersonalizationService {

    @Qualifier("rRedisTemplate")
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final PersonalizationRepository personalizationRepository;
    private final MemoryRepository memoryRepository;
    private final VectorStore vectorStore;

    // L1: In-memory User Cache (Caffeine — bounded, TTL-evicting)
    // Keyed by userId (not sessionId) because personalization data is user-level.
    // invalidateCache(userId) works correctly with this key.
    private final Cache<UUID, String> l1Cache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    private static final String REDIS_PREFIX = "personalization:";
    private static final Duration REDIS_TTL = Duration.ofHours(24);

    /**
     * Retrieves the full personalized profile for a user, following the L1 → L2 → L3 path.
     */
    @Override
    public Mono<String> getPersonalizedContext(UUID userId) {
        // 1. L1: Check in-memory cache (keyed by userId)
        String l1Hit = l1Cache.getIfPresent(userId);
        if (l1Hit != null) {
            log.debug("[Personalization] L1 HIT for user: {}", userId);
            return Mono.just(l1Hit);
        }

        // 2. L2: Check Redis
        String redisKey = REDIS_PREFIX + userId;
        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(profile -> {
                    log.debug("[Personalization] L2 HIT for user: {}", userId);
                    l1Cache.put(userId, profile);
                    return Mono.just(profile);
                })
                .switchIfEmpty(
                    // 3. L3: Merge SQL instructions + Qdrant semantic facts
                    Mono.zip(
                        getCustomInstructions(userId),
                        retrieveAndSynthesizeProfile(userId)
                    ).map(tuple -> {
                        String instructions = tuple.getT1();
                        String behaviors   = tuple.getT2();
                        return String.format(
                            "USER GUIDELINES:\n%s\n\nUSER BEHAVIORS & HISTORY:\n%s",
                            instructions, behaviors
                        );
                    }).flatMap(profile ->
                        redisTemplate.opsForValue().set(redisKey, profile, REDIS_TTL)
                            .then(Mono.fromRunnable(() -> l1Cache.put(userId, profile)))
                            .thenReturn(profile)
                    )
                );
    }

    /**
     * Fetches explicit instructions set by the user in their settings.
     */
    @Override
    public Mono<String> getCustomInstructions(UUID userId) {
        return personalizationRepository.findById(userId)
                .map(AgentPersonalization::getCustomInstructions)
                .defaultIfEmpty("No custom instructions set by user.");
    }

    /**
     * Performs a semantic search in Qdrant to find relevant historical facts about the user.
     * Qdrant is the AI's long-term memory — it is never pruned or capped.
     */
    private Mono<String> retrieveAndSynthesizeProfile(UUID userId) {
        log.debug("[Personalization] L3 CACHE MISS for user: {}. Retrieving from Vector DB...", userId);

        return Mono.fromCallable(() -> {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            SearchRequest request = SearchRequest.builder()
                    .query("Who is this user and what are their preferences?")
                    .topK(10)
                    .filterExpression(b.eq("userId", userId.toString()).build())
                    .build();

            return vectorStore.similaritySearch(request).stream()
                    .map((Document doc) -> "- " + doc.getFormattedContent())
                    .collect(Collectors.joining("\n"));
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            log.error("[Personalization] L3 Vector Search FAILED (Qdrant/Ollama down?): {}", e.getMessage());
            return Mono.just("");
        })
        .map(facts -> facts.isEmpty() ? "No specific historical data available." : facts);
    }

    /**
     * Records a new fact about the user into Qdrant (long-term semantic memory).
     * Invalidates L1/L2 caches so the next request picks up fresh context.
     *
     * <p>Note: this writes to Qdrant only — not to the SQL {@code agent_memory} table.
     * That table is managed separately with a per-user cap.
     */
    @Override
    public Mono<Void> recordBehavior(UUID userId, String fact) {
        log.debug("[Personalization] Recording behavior for user {}: {}", userId, fact);

        Document doc = new Document(fact, Map.of("userId", userId.toString()));

        return Mono.fromRunnable(() -> vectorStore.add(List.of(doc)))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.error("[Personalization] Failed to record behavior to Vector DB: {}", e.getMessage());
                    return Mono.empty();
                })
                .then(invalidateCache(userId))
                .then();
    }

    /**
     * Returns the user-visible list of AI-saved memory records from PostgreSQL, most recent first.
     */
    @Override
    public Flux<MemoryRecord> getMemories(UUID userId) {
        return memoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .map(m -> new MemoryRecord(m.getId(), m.getFact(), m.getCreatedAt()));
    }

    /**
     * Deletes a specific memory record from PostgreSQL (ownership-checked) and invalidates caches.
     *
     * <p>The corresponding Qdrant vector is intentionally NOT removed — Qdrant is the AI's
     * long-term semantic index and is managed independently of the user-visible memory list.
     */
    @Override
    public Mono<Void> deleteMemory(UUID memoryId, UUID userId) {
        return memoryRepository.findById(memoryId)
                .filter(m -> m.getUserId().equals(userId))
                .flatMap(m -> memoryRepository.deleteById(m.getId())
                        .then(invalidateCache(userId)))
                .then();
    }

    /**
     * Updates the user's explicit guidelines and invalidates caches.
     */
    @Override
    public Mono<Void> updateCustomInstructions(UUID userId, UpdateAgentConfigDto dto) {
        return personalizationRepository.findById(userId)
                .defaultIfEmpty(AgentPersonalization.builder().userId(userId).build())
                .flatMap(config -> {
                    config.setCustomInstructions(dto.customInstructions());
                    config.setTone(dto.tone());
                    return personalizationRepository.save(config);
                })
                .then(invalidateCache(userId))
                .then();
    }

    /**
     * Clears L1 (Caffeine, by userId key) and L2 (Redis) caches for the given user.
     */
    private Mono<Void> invalidateCache(UUID userId) {
        String redisKey = REDIS_PREFIX + userId;
        return redisTemplate.delete(redisKey)
                .then(Mono.fromRunnable(() -> l1Cache.invalidate(userId)))
                .then();
    }
}
