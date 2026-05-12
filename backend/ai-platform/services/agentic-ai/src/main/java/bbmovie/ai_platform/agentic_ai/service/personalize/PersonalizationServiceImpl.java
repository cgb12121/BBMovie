package bbmovie.ai_platform.agentic_ai.service.personalize;

import bbmovie.ai_platform.agentic_ai.dto.request.UpdateAgentConfigDto;
import bbmovie.ai_platform.agentic_ai.entity.AgentPersonalization;
import bbmovie.ai_platform.agentic_ai.repository.PersonalizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalizationServiceImpl implements PersonalizationService {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final PersonalizationRepository personalizationRepository;
    private final VectorStore vectorStore;

    // L1: In-memory Session Cache
    private final Map<UUID, String> l1Cache = new ConcurrentHashMap<>();

    private static final String REDIS_PREFIX = "personalization:";
    private static final Duration REDIS_TTL = Duration.ofHours(24);

    @Override
    public Mono<String> getPersonalizedContext(UUID userId, UUID sessionId) {
        // 1. Check L1 (In-memory Session Cache)
        if (l1Cache.containsKey(sessionId)) {
            log.debug("[Personalization] L1 HIT for session: {}", sessionId);
            return Mono.just(l1Cache.get(sessionId));
        }

        // 2. Check L2 (Redis User Cache)
        String redisKey = REDIS_PREFIX + userId;
        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(profile -> {
                    log.debug("[Personalization] L2 HIT for user: {}", userId);
                    l1Cache.put(sessionId, profile);
                    return Mono.just(profile);
                })
                .switchIfEmpty(
                    // 3. Check L3 (Merge DB Settings + Vector DB Semantic Search)
                    Mono.zip(
                        getCustomInstructions(userId),
                        retrieveAndSynthesizeProfile(userId)
                    ).map(tuple -> {
                        String instructions = tuple.getT1();
                        String behaviors = tuple.getT2();
                        
                        return String.format(
                            "USER GUIDELINES:\n%s\n\nUSER BEHAVIORS & HISTORY:\n%s",
                            instructions, behaviors
                        );
                    }).flatMap(profile -> 
                        redisTemplate.opsForValue().set(redisKey, profile, REDIS_TTL)
                            .then(Mono.fromRunnable(() -> l1Cache.put(sessionId, profile)))
                            .thenReturn(profile)
                    )
                );
    }

    @Override
    public Mono<String> getCustomInstructions(UUID userId) {
        return personalizationRepository.findById(userId)
                .map(AgentPersonalization::getCustomInstructions)
                .defaultIfEmpty("No custom instructions set by user.");
    }

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
        .map(facts -> facts.isEmpty() ? "No specific historical data available." : facts);
    }

    @Override
    public Mono<Void> recordBehavior(UUID userId, String fact) {
        log.debug("[Personalization] Recording and Indexing behavior for user {}: {}", userId, fact);
        
        Document doc = new Document(fact, Map.of("userId", userId.toString()));
        
        return Mono.fromRunnable(() -> vectorStore.add(List.of(doc)))
                .subscribeOn(Schedulers.boundedElastic())
                .then(invalidateCache(userId))
                .then();
    }

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

    private Mono<Void> invalidateCache(UUID userId) {
        String redisKey = REDIS_PREFIX + userId;
        return redisTemplate.delete(redisKey)
                .then(Mono.fromRunnable(l1Cache::clear))
                .then();
    }
}
