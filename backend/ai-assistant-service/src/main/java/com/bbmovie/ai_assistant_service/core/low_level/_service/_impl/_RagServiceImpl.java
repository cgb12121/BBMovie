package com.bbmovie.ai_assistant_service.core.low_level._service._impl;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import com.bbmovie.ai_assistant_service.core.low_level._config._embedding._EmbeddingSelector;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._dto._RagMovieDto;
import com.bbmovie.ai_assistant_service.core.low_level._dto._RagRetrievalResult;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._RagService;
import com.bbmovie.ai_assistant_service.core.low_level._utils._MetricsUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
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

    // Hybrid Movie Context Retrieval
    @Override
    public Mono<_RagRetrievalResult> retrieveMovieContext(UUID sessionId, String query, int topK) {
        return embedText(sessionId, query, _InteractionType.EMBEDDING)
                .flatMap(vector -> hybridSearch(sessionId, vector, topK))
                .map(movies -> {
                    if (movies.isEmpty()) {
                        return new _RagRetrievalResult("", List.of());
                    }

                    log.debug("[rag] Retrieved movies: {}", movies);
                    // Build textual summary for LLM
                    String contextText = movies.stream()
                            .map(_RagServiceImpl::formatMovieDetails)
                            .collect(Collectors.joining("\n\n"));

                    return new _RagRetrievalResult(contextText, movies);
                })
                .onErrorResume(ex -> {
                    log.warn("[rag] Retrieval failed: {}", ex.getMessage());
                    return Mono.just(new _RagRetrievalResult("", List.of()));
                });
    }

    // Index Conversation Fragment
    @Override
    public Mono<Void> indexConversationFragment(UUID sessionId, String text, List<_RagMovieDto> pastResults) {
        String pastResultsString = pastResults.stream()
                .flatMap(_RagServiceImpl::getMovieOverview)
                .collect(Collectors.joining("\n"));
        return embedText(sessionId, text + pastResultsString, _InteractionType.EMBEDDING_INDEX)
                .flatMap(vector -> {
                    if (embeddingSelector.getDimension() != vector.length) {
                        return Mono.error(new IllegalArgumentException(
                                "Embedding dimension mismatch: expected " +
                                        embeddingSelector.getDimension() + " but got " + vector.length)
                        );
                    }

                    Map<String, Object> doc = Map.of(
                            "id", UUID.randomUUID().toString(),
                            "sessionId", sessionId.toString(),
                            "timestamp", Instant.now().toString(),
                            "text", text,
                            "movie", pastResults,
                            embeddingSelector.getEmbeddingField(), vector
                    );

                    IndexRequest<Map<String, Object>> request = IndexRequest.of(i -> i
                            .index(embeddingSelector.getRagIndex())
                            .document(doc)
                    );

                    return Mono.fromFuture(esClient.index(request))
                            .doOnSuccess(r -> {
                                String ragIndex = embeddingSelector.getRagIndex();
                                log.debug("[rag] Indexed chat fragment into '{}'", ragIndex);
                            })
                            .then(auditService.recordInteraction(
                                    sessionId, _InteractionType.EMBEDDING_INDEX,
                                    Map.of("text", text.substring(0, Math.min(80, text.length())) + "..."),
                                    _MetricsUtil.get(0L, null, embeddingSelector.getModelName(), "rag-index")
                            ));
                })
                .doOnError(e -> log.error("[rag] Failed to index fragment: {}", e.getMessage()));
    }

    // Hybrid Search — combines movies + memory indices (not memory rn)
    private Mono<List<_RagMovieDto>> hybridSearch(UUID sessionId, float[] embedding, int topK) {
        long start = System.currentTimeMillis();

        // Only search the movie index now
        return performSearch(sessionId, embedding, topK, embeddingSelector.getMovieIndex(), "movies")
                .flatMap(results -> {
                    long latency = System.currentTimeMillis() - start;

                    _Metrics metrics = _MetricsUtil.get(latency, null,
                            embeddingSelector.getModelName(), "rag-movie-search");

                    return auditService.recordInteraction(
                                    sessionId,
                                    _InteractionType.RETRIEVAL,
                                    Map.of(
                                            "index", embeddingSelector.getMovieIndex(),
                                            "topK", topK,
                                            "results", results.size()
                                    ),
                                    metrics
                            )
                            .thenReturn(results);
                })
                .doOnSuccess(results -> log.debug("[rag] Movie retrieval completed."))
                .doOnError(e -> log.error("[rag] Error during movie search: {}", e.getMessage(), e));
    }


    // Perform a Vector Search on a Specific Index
    @SuppressWarnings("SameParameterValue") // Will be used for hybrid search
    private Mono<List<_RagMovieDto>> performSearch(
            UUID sessionId, float[] embedding, int topK, String index, String label) {
        long start = System.currentTimeMillis();

        Query query = Query.of(q -> q.scriptScore(ss -> ss
                .query(q2 -> q2.matchAll(m -> m))
                .script(s -> s
                        .source("cosineSimilarity(params.query_vector, '" +
                                embeddingSelector.getEmbeddingField() + "') + 1.0")
                        .params(Map.of("query_vector", JsonData.of(embedding))))));

        SearchRequest request = SearchRequest.of(s -> s.index(index).size(topK).query(query));

        return Mono.fromFuture(esClient.search(request, Map.class))
                .map(resp -> resp.hits()
                        .hits()
                        .stream()
                        .map(hit -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> source = hit.source();
                            return toRagMovieDto(source);
                        })
                        .filter(Objects::nonNull)
                        .toList())
                .flatMap(results -> {
                    long latency = System.currentTimeMillis() - start;
                    _Metrics metrics = _MetricsUtil.get(latency, null,
                            embeddingSelector.getModelName(),"rag-search-" + label);

                    return auditService.recordInteraction(
                            sessionId, _InteractionType.RETRIEVAL,
                            Map.of(
                                    "index", index,
                                    "hits", results.size()
                            ), metrics)
                            .thenReturn(results);
                })
                .doOnSuccess(list -> log.debug("[rag] [{}] Found {} hits", label, list.size()))
                .doOnError(e -> log.error("[rag] [{}] Search failed: {}", label, e.getMessage()));
    }

    //   Embedding helper — clean separation
    private Mono<float[]> embedText(UUID sessionId, String text, _InteractionType type) {
        long start = System.currentTimeMillis();

        return Mono.fromCallable(() -> embeddingModel.embed(text))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(embedding -> {
                    long latency = System.currentTimeMillis() - start;
                    TokenUsage usage = embedding.tokenUsage();

                    _Metrics metrics = _MetricsUtil.get(latency, usage,
                            embeddingSelector.getModelName(), "embedding");

                    return auditService.recordInteraction(sessionId, type, Map.of("text", text), metrics)
                            .thenReturn(embedding.content().vector());
                });
    }

    private static @NonNull Stream<String> getMovieOverview(_RagMovieDto m) {
        return Stream.of(
                "Title: " + m.getTitle(),
                "Release Year: " + (m.getReleaseYear() != null ? m.getReleaseYear() : "N/A"),
                "Genres: " + (m.getGenres() != null ? String.join(", ", m.getGenres()) : "N/A"),
                "Directors: " + (m.getDirectors() != null ? String.join(", ", m.getDirectors()) : "N/A"),
                "Description: " + m.getDescription(),
                "ID: " + m.getId(),
                ""
        );
    }

    private static @NonNull String formatMovieDetails(_RagMovieDto m) {
        return String.format(
                "%s (%s) — %s\nDirector(s): %s\nDescription: %s\nID: %s",
                m.getTitle(),
                m.getReleaseYear() != null ? m.getReleaseYear() : "N/A",
                String.join(", ", m.getGenres() != null ? m.getGenres() : List.of("unknown genre")),
                m.getDirectors() != null ? String.join(", ", m.getDirectors()) : "N/A",
                m.getDescription(),
                m.getId()
        );
    }

    private _RagMovieDto toRagMovieDto(Map<String, Object> source) {
        try {
            return objectMapper.convertValue(source, _RagMovieDto.class);
        } catch (Exception e) {
            log.warn("[rag] Failed to map document to _RagMovieDto: {}", e.getMessage());
            return null;
        }
    }
}
