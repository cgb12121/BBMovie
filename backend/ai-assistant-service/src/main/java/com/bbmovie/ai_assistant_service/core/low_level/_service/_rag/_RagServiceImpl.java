package com.bbmovie.ai_assistant_service.core.low_level._service._rag;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import com.bbmovie.ai_assistant_service.core.low_level._config._embedding._EmbeddingSelector;
import com.bbmovie.ai_assistant_service.core.low_level._dto._ChatMetrics;
import com.bbmovie.ai_assistant_service.core.low_level._dto._RagMovieDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto._RagRetrievalResult;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class _RagServiceImpl implements _RagService {

    private final EmbeddingModel embeddingModel;
    private final ElasticsearchAsyncClient esClient;
    private final _EmbeddingSelector embeddingSelector;
    private final _AuditService auditService;
    private final ObjectMapper objectMapper;

    @Autowired
    public _RagServiceImpl(
            @Qualifier("_EmbeddingModel") EmbeddingModel embeddingModel,
            @Qualifier("_ElasticsearchAsyncClient") ElasticsearchAsyncClient esClient,
            @Qualifier("_ObjectMapper") ObjectMapper objectMapper,
            _EmbeddingSelector embeddingSelector,
            _AuditService auditService) {
        this.embeddingModel = embeddingModel;
        this.esClient = esClient;
        this.objectMapper = objectMapper;
        this.embeddingSelector = embeddingSelector;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------
    // 🔹 1. Hybrid Movie Context Retrieval
    // ------------------------------------------------------------
    @Override
    public Mono<_RagRetrievalResult> retrieveMovieContext(UUID sessionId, String query, int topK) {
        return embedText(sessionId, query, _InteractionType.EMBEDDING)
                .flatMap(vector -> hybridSearch(sessionId, vector, topK))
                .map(movies -> {
                    if (movies.isEmpty()) {
                        return new _RagRetrievalResult("", List.of());
                    }

                    // Build textual summary for LLM
                    String contextText = movies.stream()
                            .map(m -> String.format(
                                    "%s (%s) — %s\nDirector(s): %s\nDescription: %s\nID: %s",
                                    m.getTitle(),
                                    m.getReleaseYear(),
                                    String.join(", ", m.getGenre() != null ? m.getGenre() : List.of()),
                                    m.getDirectors() != null ? String.join(", ", m.getDirectors()) : "N/A",
                                    m.getDescription(),
                                    m.getId()
                            ))
                            .collect(Collectors.joining("\n\n"));

                    return new _RagRetrievalResult(contextText, movies);
                })
                .onErrorResume(ex -> {
                    log.warn("[rag] Retrieval failed: {}", ex.getMessage());
                    return Mono.just(new _RagRetrievalResult("", List.of()));
                });
    }


    // ------------------------------------------------------------
    // 🔹 2. Index Conversation Fragment
    // ------------------------------------------------------------
    @Override
    public Mono<Void> indexConversationFragment(UUID sessionId, String text) {
        return embedText(sessionId, text, _InteractionType.EMBEDDING_INDEX)
                .flatMap(vector -> {
                    if (embeddingSelector.getDimension() != vector.length) {
                        return Mono.error(new IllegalArgumentException(
                                "Embedding dimension mismatch: expected " +
                                        embeddingSelector.getDimension() + " but got " + vector.length));
                    }

                    Map<String, Object> doc = Map.of(
                            "id", UUID.randomUUID().toString(),
                            "sessionId", sessionId.toString(),
                            "timestamp", Instant.now().toString(),
                            "text", text,
                            embeddingSelector.getEmbeddingField(), vector
                    );

                    IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                            .index(embeddingSelector.getRagIndex())
                            .document(doc)
                    );

                    return Mono.fromFuture(esClient.index(request))
                            .doOnSuccess(r -> log.debug("[rag] Indexed chat fragment into '{}'", embeddingSelector.getRagIndex()))
                            .then(auditService.recordInteraction(
                                    sessionId,
                                    _InteractionType.EMBEDDING_INDEX,
                                    Map.of("text", text.substring(0, Math.min(80, text.length())) + "..."),
                                    _ChatMetrics.builder()
                                            .modelName(embeddingSelector.getModelName())
                                            .tool("embedding-index")
                                            .build()
                            ));
                })
                .doOnError(e -> log.error("[rag] Failed to index fragment: {}", e.getMessage()));
    }

    // ------------------------------------------------------------
    // 🔹 3. Hybrid Search — combines movies + memory indices
    // ------------------------------------------------------------
    private Mono<List<_RagMovieDto>> hybridSearch(UUID sessionId, float[] embedding, int topK) {
        long start = System.currentTimeMillis();

        Mono<List<_RagMovieDto>> movieSearch = performSearch(sessionId, embedding, topK, embeddingSelector.getMovieIndex(), "movies");
        Mono<List<_RagMovieDto>> memorySearch = performSearch(sessionId, embedding, topK, embeddingSelector.getRagIndex(), "user-memory");

        return Mono.zipDelayError(movieSearch, memorySearch)
                .map(tuple -> Stream.concat(tuple.getT1().stream(), tuple.getT2().stream())
                        .distinct()
                        .limit(topK)
                        .toList())
                .flatMap(results -> {
                    long latency = System.currentTimeMillis() - start;

                    _ChatMetrics metrics = _ChatMetrics.builder()
                            .latencyMs(latency)
                            .tool("hybrid-rag")
                            .modelName(embeddingSelector.getModelName())
                            .responseTokens(results.size())
                            .build();

                    return auditService.recordInteraction(
                                    sessionId,
                                    _InteractionType.RETRIEVAL,
                                    Map.of("topK", topK, "results", results.size()),
                                    metrics
                            )
                            .thenReturn(results);
                });
    }

    // ------------------------------------------------------------
    // 🔹 4. Perform a Vector Search on a Specific Index
    // ------------------------------------------------------------
    private Mono<List<_RagMovieDto>> performSearch(UUID sessionId, float[] embedding, int topK, String index, String label) {
        long start = System.currentTimeMillis();

        Query query = Query.of(q -> q.scriptScore(ss -> ss
                .query(q2 -> q2.matchAll(m -> m))
                .script(s -> s
                        .source("cosineSimilarity(params.query_vector, '" +
                                embeddingSelector.getEmbeddingField() + "') + 1.0")
                        .params(Map.of("query_vector", JsonData.of(embedding))))));

        SearchRequest request = SearchRequest.of(s -> s.index(index).size(topK).query(query));

        return Mono.fromFuture(esClient.search(request, Map.class))
                .map(resp -> resp.hits().hits().stream()
                        .map(hit -> toRagMovieDto(hit.source()))
                        .filter(Objects::nonNull)
                        .toList())
                .flatMap(results -> {
                    long latency = System.currentTimeMillis() - start;
                    _ChatMetrics metrics = _ChatMetrics.builder()
                            .latencyMs(latency)
                            .modelName(embeddingSelector.getModelName())
                            .tool("rag-search-" + label)
                            .responseTokens(results.size())
                            .build();

                    return auditService.recordInteraction(
                                    sessionId,
                                    _InteractionType.RETRIEVAL,
                                    Map.of("index", index, "hits", results.size()),
                                    metrics)
                            .thenReturn(results);
                })
                .doOnSuccess(list -> log.debug("[rag] [{}] Found {} hits", label, list.size()))
                .doOnError(e -> log.error("[rag] [{}] Search failed: {}", label, e.getMessage()));
    }

    // ------------------------------------------------------------
    // 🔹 5. Embedding helper — clean separation
    // ------------------------------------------------------------
    private Mono<float[]> embedText(UUID sessionId, String text, _InteractionType type) {
        long start = System.currentTimeMillis();

        return Mono.fromCallable(() -> embeddingModel.embed(text))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(embedding -> {
                    long latency = System.currentTimeMillis() - start;
                    TokenUsage usage = embedding.tokenUsage();

                    _ChatMetrics metrics = _ChatMetrics.builder()
                            .latencyMs(latency)
                            .promptTokens(Optional.ofNullable(usage).map(TokenUsage::inputTokenCount).orElse(0))
                            .responseTokens(Optional.ofNullable(usage).map(TokenUsage::outputTokenCount).orElse(0))
                            .modelName(embeddingSelector.getModelName())
                            .tool("embedding")
                            .build();

                    return auditService.recordInteraction(
                                    sessionId,
                                    type,
                                    Map.of("text", text.substring(0, Math.min(80, text.length())) + "..."),
                                    metrics)
                            .thenReturn(embedding.content().vector());
                });
    }

    // ------------------------------------------------------------
    // 🔹 6. Safe Mapping to DTO
    // ------------------------------------------------------------
    private _RagMovieDto toRagMovieDto(Map<String, Object> source) {
        try {
            return objectMapper.convertValue(source, _RagMovieDto.class);
        } catch (Exception e) {
            log.warn("[rag] Failed to map document to _RagMovieDto: {}", e.getMessage());
            return null;
        }
    }
}
