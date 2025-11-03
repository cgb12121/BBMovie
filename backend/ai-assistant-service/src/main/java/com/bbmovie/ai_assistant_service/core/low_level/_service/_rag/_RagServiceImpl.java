package com.bbmovie.ai_assistant_service.core.low_level._service._rag;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import com.bbmovie.ai_assistant_service.core.low_level._config._embedding._EmbeddingSelector;
import com.bbmovie.ai_assistant_service.core.low_level._dto._ChatMetrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
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
import java.util.stream.Stream;

@Slf4j
@Service
public class _RagServiceImpl implements _RagService {

    private final EmbeddingModel embeddingModel;
    private final ElasticsearchAsyncClient esClient;
    private final _EmbeddingSelector embeddingSelector;
    private final _AuditService auditService;

    @Autowired
    public _RagServiceImpl(
            @Qualifier("_EmbeddingModel") EmbeddingModel embeddingModel,
            @Qualifier("_ElasticsearchAsyncClient") ElasticsearchAsyncClient esClient,
            _EmbeddingSelector embeddingSelector,
            _AuditService auditService) {
        this.embeddingModel = embeddingModel;
        this.esClient = esClient;
        this.embeddingSelector = embeddingSelector;
        this.auditService = auditService;
    }

    /**
     * Hybrid retrieval: combines movie metadata + user conversation memory.
     */
    @Override
    public Mono<List<String>> retrieveMovieContext(UUID sessionId, String query, int topK) {
        long start = System.currentTimeMillis();

        return Mono.fromCallable(() -> embeddingModel.embed(query))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(embedding -> {
                    long latency = System.currentTimeMillis() - start;
                    TokenUsage usage = embedding.tokenUsage();

                    int inputTokenCount = Optional.ofNullable(usage)
                            .map(TokenUsage::inputTokenCount)
                            .orElse(0);
                    int outputTokenCount = Optional.ofNullable(usage)
                            .map(TokenUsage::outputTokenCount)
                            .orElse(0);

                    _ChatMetrics metrics = _ChatMetrics.builder()
                            .latencyMs(latency)
                            .promptTokens(inputTokenCount)
                            .responseTokens(outputTokenCount)
                            .modelName(embeddingSelector.getModelName())
                            .tool("embedding")
                            .build();

                    // Audit the embedding creation step
                    return auditService.recordInteraction(
                                    sessionId,
                                    _InteractionType.EMBEDDING,
                                    Map.of(
                                            "query", query,
                                            "vectorDim", embedding.content().vector().length
                                    ),
                                    metrics
                            )
                            .thenReturn(embedding.content().vector());
                })
                .flatMap(vector -> hybridSearch(sessionId, vector, topK))
                .doOnError(e -> log.error("[rag] Error during retrieveMovieContext: {}", e.getMessage(), e));
    }

    /**
     * Index a conversation fragment into the RAG conversation index.
     */
    @Override
    public Mono<Void> indexConversationFragment(UUID sessionId, String text) {
        long start = System.currentTimeMillis();

        return Mono.fromCallable(() -> embeddingModel.embed(text))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(embedding -> {
                    long latency = System.currentTimeMillis() - start;
                    TokenUsage usage = embedding.tokenUsage();

                    int inputTokenCount = Optional.ofNullable(usage)
                            .map(TokenUsage::inputTokenCount)
                            .orElse(0);
                    int outputTokenCount = Optional.ofNullable(usage)
                            .map(TokenUsage::outputTokenCount)
                            .orElse(0);

                    _ChatMetrics metrics = _ChatMetrics.builder()
                            .latencyMs(latency)
                            .promptTokens(inputTokenCount)
                            .responseTokens(outputTokenCount)
                            .modelName(embeddingSelector.getModelName())
                            .tool("embedding")
                            .build();

                    return indexConversation(sessionId, text, embedding.content().vector())
                            .then(auditService.recordInteraction(
                                    sessionId,
                                    _InteractionType.EMBEDDING_INDEX,
                                    Map.of("text", text.substring(0, Math.min(80, text.length())) + "..."),
                                    metrics
                            ));
                })
                .doOnSuccess(v -> log.debug("[rag] Indexed fragment for session {}", sessionId))
                .doOnError(e -> log.error("[rag] Error indexing fragment: {}", e.getMessage(), e));
    }

    private Mono<Void> indexConversation(UUID sessionId, String text, float[] embedding) {
        if (embedding.length != embeddingSelector.getDimension()) {
            return Mono.error(new IllegalArgumentException(String.format(
                    "Embedding dimension mismatch: expected %d but got %d",
                    embeddingSelector.getDimension(), embedding.length
            )));
        }

        Map<String, Object> doc = Map.of(
                "id", UUID.randomUUID().toString(),
                "sessionId", sessionId.toString(),
                "timestamp", Instant.now().toString(),
                "text", text,
                embeddingSelector.getEmbeddingField(), embedding
        );

        IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                .index(embeddingSelector.getRagIndex())
                .document(doc)
        );

        return Mono.fromFuture(esClient.index(request))
                .doOnSuccess(r -> log.debug("[rag] Indexed chat fragment into '{}'", embeddingSelector.getRagIndex()))
                .then();
    }

    /**
     * Hybrid RAG retrieval: merges results from movie metadata and user chat memory.
     */
    private Mono<List<String>> hybridSearch(UUID sessionId, float[] embedding, int topK) {
        long startTime = System.currentTimeMillis();
        // Run both searches in parallel
        Mono<List<ScoredDocument>> movieResults = searchWithScores(
                sessionId, embedding, topK, embeddingSelector.getMovieIndex(), "movies");
        Mono<List<ScoredDocument>> ragResults = searchWithScores(
                sessionId, embedding, topK, embeddingSelector.getRagIndex(), "user-memory");

        return Mono.zip(movieResults, ragResults)
                .map(tuple -> Stream.concat(tuple.getT1().stream(), tuple.getT2().stream())
                        .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
                        .limit(topK)
                        .map(ScoredDocument::text)
                        .toList())
                .flatMap(results -> {
                    long latency = System.currentTimeMillis() - startTime;
                    // Combine and audit merged retrieval
                    _ChatMetrics combinedMetrics = _ChatMetrics.builder()
                            .latencyMs(latency)
                            .tool("hybrid-rag")
                            .modelName(embeddingSelector.getModelName())
                            .responseTokens(results.size())
                            .build();

                    return auditService.recordInteraction(
                                    sessionId,
                                    _InteractionType.RETRIEVAL,
                                    Map.of("topK", topK, "totalResults", results.size()),
                                    combinedMetrics
                            )
                            .thenReturn(results);
                });
    }

    /**
     * Performs a similarity search in a specific index (movies or rag_conversations).
     */
    private Mono<List<ScoredDocument>> searchWithScores(UUID sessionId, float[] embedding, int topK, String index, String label) {
        long start = System.currentTimeMillis();

        Query query = Query.of(q -> q
                .scriptScore(ss -> ss
                        .query(q2 -> q2.matchAll(m -> m))
                        .script(s -> s
                                .source("cosineSimilarity(params.query_vector, '" +
                                        embeddingSelector.getEmbeddingField() + "') + 1.0")
                                .params(Map.of("query_vector", JsonData.of(embedding)))
                        )
                )
        );

        SearchRequest request = SearchRequest.of(s -> s
                .index(index)
                .size(topK)
                .query(query)
        );

        return Mono.fromFuture(esClient.search(request, Map.class))
                .flatMap(response -> {
                    long latency = System.currentTimeMillis() - start;

                    List<ScoredDocument> results = response.hits().hits().stream()
                            .map(hit -> new ScoredDocument(
                                    Optional.ofNullable((Map<?, ?>) hit.source())
                                            .map(s -> (String) s.get("text"))
                                            .orElse(""),
                                    hit.score() == null ? 0.0 : hit.score()
                            ))
                            .toList();

                    _ChatMetrics metrics = _ChatMetrics.builder()
                            .latencyMs(latency)
                            .modelName(embeddingSelector.getModelName())
                            .tool("rag-search-" + label)
                            .responseTokens(results.size())
                            .build();

                    return auditService.recordInteraction(
                                    sessionId,
                                    _InteractionType.RETRIEVAL,
                                    Map.of("index", index, "topK", topK, "hits", results.size()),
                                    metrics
                            )
                            .thenReturn(results);
                })
                .doOnSuccess(list -> log.debug("[rag] [{}] Found {} hits", label, list.size()))
                .doOnError(e -> log.error("[rag] [{}] Error during search: {}", label, e.getMessage(), e));
    }

    /**
     * Internal holder for search results with scores.
     */
    private record ScoredDocument(String text, double score) {}
}