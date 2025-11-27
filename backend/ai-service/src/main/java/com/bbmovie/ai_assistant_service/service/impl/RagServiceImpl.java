package com.bbmovie.ai_assistant_service.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.JsonData;
import com.bbmovie.ai_assistant_service.config.embedding.EmbeddingSelector;
import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import com.bbmovie.ai_assistant_service.dto.Metrics;
import com.bbmovie.ai_assistant_service.dto.response.RagMovieDto;
import com.bbmovie.ai_assistant_service.dto.response.RagRetrievalResult;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.service.RagService;
import com.bbmovie.ai_assistant_service.utils.MetricsUtil;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.TokenUsage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(RagServiceImpl.class);

    @Qualifier("_EmbeddingModel") private final EmbeddingModel embeddingModel;
    @Qualifier("elasticsearchAsyncClient") private final ElasticsearchAsyncClient esClient;
    private final EmbeddingSelector embeddingSelector;
    private final AuditService auditService;
    @Qualifier("ObjectMapper") private final ObjectMapper objectMapper;

    // Hybrid Movie Context Retrieval
    @Override
    public Mono<RagRetrievalResult> retrieveMovieContext(UUID sessionId, String query, int topK) {
        return embedText(sessionId, query, InteractionType.EMBEDDING)
                .flatMap(vector -> hybridSearch(sessionId, vector, topK))
                .map(movies -> {
                    if (movies.isEmpty()) {
                        return new RagRetrievalResult("", List.of());
                    }

                    log.debug("[rag] Retrieved movies: {}", movies.size());
                    // Build textual summary for LLM
                    String contextText = movies.stream()
                            .map(RagServiceImpl::formatMovieDetails)
                            .collect(Collectors.joining("\n\n"));

                    return new RagRetrievalResult(contextText, movies);
                })
                .onErrorResume(ex -> {
                    log.warn("[rag] Retrieval failed: {}", ex.getMessage());
                    return Mono.just(new RagRetrievalResult("", List.of()));
                });
    }

    // Index Conversation Fragment
    @Override
    public Mono<Void> indexConversationFragment(UUID sessionId, String text, List<RagMovieDto> pastResults) {
        String pastResultsString = pastResults.stream()
                .flatMap(RagServiceImpl::getMovieOverview)
                .collect(Collectors.joining("\n"));
        return embedText(sessionId, text + pastResultsString, InteractionType.EMBEDDING_INDEX)
                .flatMap(vector -> {
                    if (embeddingSelector.getDimension() != vector.length) {
                        String err = "Embedding dimension mismatch: expected %s but got %s"
                                .formatted(embeddingSelector.getDimension(), vector.length);
                        log.error("[rag] Embedding index failed: {}", err);
                        return Mono.error(new IllegalArgumentException(err)
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
                                    AuditRecord.builder()
                                            .sessionId(sessionId)
                                            .type(InteractionType.EMBEDDING_INDEX)
                                            .details(Map.of("text", text.substring(0, Math.min(80, text.length())) + "..."))
                                            .metrics(MetricsUtil.get(0L, null, embeddingSelector.getModelName(), "rag-index"))
                                            .build()
                            ));
                })
                .doOnError(e -> log.error("[rag] Failed to index fragment: {}", e.getMessage()));
    }

    // Hybrid Search — combines movies and memory indices (not memory rn)
    private Mono<List<RagMovieDto>> hybridSearch(UUID sessionId, float[] embedding, int topK) {
        long start = System.currentTimeMillis();

        // Only search the movie index now using native KNN
        return nativeKnn(sessionId, embedding, topK, embeddingSelector.getMovieIndex(), "movies")
                .flatMap(results -> {
                    long latency = System.currentTimeMillis() - start;

                    Metrics metrics = MetricsUtil.get(latency, null,
                            embeddingSelector.getModelName(), "rag-movie-search");

                    AuditRecord auditRecord = AuditRecord.builder()
                            .sessionId(sessionId)
                            .type(InteractionType.RETRIEVAL)
                            .details(Map.of(
                                    "index", embeddingSelector.getMovieIndex(),
                                    "topK", topK,
                                    "results", results.size(),
                                    "search_type", "knn"
                            ))
                            .metrics(metrics)
                            .build();
                    return auditService.recordInteraction(auditRecord)
                            .thenReturn(results);
                })
                .doOnSuccess(results -> log.debug("[rag] Movie retrieval completed."))
                .doOnError(e -> log.error("[rag] Error during movie search: {}", e.getMessage(), e));
    }

    // Perform a native KNN vector search on a specific index
    @SuppressWarnings("SameParameterValue")
    private Mono<List<RagMovieDto>> nativeKnn(UUID sessionId, float[] embedding, int topK, String index, String label) {
        long start = System.currentTimeMillis();

        List<Float> vector = new ArrayList<>();
        for (float f : embedding) {
            vector.add(f);
        }

        KnnSearch knn = KnnSearch.of(k -> k
                .field(embeddingSelector.getEmbeddingField())
                .queryVector(vector)
                .k(topK)
                .numCandidates(topK * 10)
        );

        SearchRequest request = SearchRequest.of(s -> s
                .index(index)
                .knn(List.of(knn))
                .size(topK)
        );

        return Mono.fromFuture(esClient.search(request, Map.class))
                .map(resp -> resp.hits().hits().stream()
                        .map(hit -> {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> source = hit.source();
                            return toRagMovieDto(source);
                        })
                        .filter(Objects::nonNull)
                        .toList())
                .flatMap(results -> {
                    long latency = System.currentTimeMillis() - start;
                    Metrics metrics = MetricsUtil.get(latency, null,
                            embeddingSelector.getModelName(), "rag-knn-search-" + label);

                    AuditRecord auditRecord = AuditRecord.builder()
                            .sessionId(sessionId)
                            .type(InteractionType.RETRIEVAL)
                            .details(Map.of(
                                    "index", index,
                                    "hits", results.size(),
                                    "search_type", "knn"
                            ))
                            .metrics(metrics)
                            .build();
                    return auditService.recordInteraction(auditRecord)
                            .thenReturn(results);
                })
                .doOnSuccess(list -> log.debug("[rag] [{}] Native KNN found {} hits", label, list.size()))
                .doOnError(e -> log.error("[rag] [{}] Native KNN search failed: {}", label, e.getMessage()));
    }

    // Fallback when native knn fails
    @SuppressWarnings({"SameParameterValue", "unused"})
    private Mono<List<RagMovieDto>> performSearch(
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
                    Metrics metrics = MetricsUtil.get(latency, null,
                            embeddingSelector.getModelName(),"rag-search-" + label);

                    AuditRecord auditRecord = AuditRecord.builder()
                            .sessionId(sessionId)
                            .type(InteractionType.RETRIEVAL)
                            .details(Map.of(
                                    "index", index,
                                    "hits", results.size()
                            ))
                            .metrics(metrics)
                            .build();
                    return auditService.recordInteraction(auditRecord)
                            .thenReturn(results);
                })
                .doOnSuccess(list -> log.debug("[rag] [{}] Found {} hits", label, list.size()))
                .doOnError(e -> log.error("[rag] [{}] Search failed: {}", label, e.getMessage()));
    }

    //   Embedding helper — clean separation
    private Mono<float[]> embedText(UUID sessionId, String text, InteractionType type) {
        long start = System.currentTimeMillis();

        return Mono.fromCallable(() -> embeddingModel.embed(text))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(embedding -> {
                    long latency = System.currentTimeMillis() - start;
                    TokenUsage usage = embedding.tokenUsage();

                    Metrics metrics = MetricsUtil.get(latency, usage,
                            embeddingSelector.getModelName(), "embedding");

                    AuditRecord auditRecord = AuditRecord.builder()
                            .sessionId(sessionId)
                            .type(type)
                            .details(Map.of("text", text))
                            .metrics(metrics)
                            .build();
                    return auditService.recordInteraction(auditRecord)
                            .thenReturn(embedding.content().vector());
                });
    }

    private static @NonNull Stream<String> getMovieOverview(RagMovieDto m) {
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

    private static @NonNull String formatMovieDetails(RagMovieDto m) {
        return String.format(
                "%s (%s) — %s%nDirector(s): %s%nDescription: %s%nID: %s",
                m.getTitle(),
                m.getReleaseYear() != null ? m.getReleaseYear() : "N/A",
                String.join(", ", m.getGenres() != null ? m.getGenres() : List.of("unknown genre")),
                m.getDirectors() != null ? String.join(", ", m.getDirectors()) : "N/A",
                m.getDescription(),
                m.getId()
        );
    }

    private RagMovieDto toRagMovieDto(Map<String, Object> source) {
        try {
            return objectMapper.convertValue(source, RagMovieDto.class);
        } catch (Exception e) {
            log.warn("[rag] Failed to map document to _RagMovieDto: {}", e.getMessage());
            return null;
        }
    }
}
