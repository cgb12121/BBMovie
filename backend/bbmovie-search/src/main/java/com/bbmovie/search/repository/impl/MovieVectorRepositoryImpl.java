package com.bbmovie.search.repository.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.bbmovie.search.dto.PageResponse;
import com.bbmovie.search.entity.MovieDocument;
import com.bbmovie.search.repository.MovieVectorRepository;
import com.bbmovie.search.service.embedding.EmbeddingService;
import com.bbmovie.search.utils.EmbeddingUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.document.Document;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Repository
public class MovieVectorRepositoryImpl implements MovieVectorRepository {

    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private static final double MIN_SIMILARITY = 0.8;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    public MovieVectorRepositoryImpl(VectorStore vectorStore, EmbeddingService embeddingService) {
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
    }

    @Override
    public Mono<Void> save(MovieDocument movie) {
        return embeddingService.generateEmbedding(movie.getDescription())
                .flatMap(vector -> {
                    movie.setEmbedding(vector);
                    Document doc = Document.builder()
                            .id(movie.getId())
                            .metadata(Map.of(
                                    "title", movie.getTitle(),
                                    "rating", movie.getRating(),
                                    "categories", movie.getCategories(),
                                    "description", movie.getDescription()
                            ))
                            .build();
                    return Mono.fromRunnable(() -> vectorStore.add(List.of(doc)));
                })
                .then();
    }

    @Override
    public Mono<PageResponse<MovieDocument>> searchByText(String query, int page, int size) {
        return embeddingService.generateEmbedding(query)
                        .flatMap(queryEmbedding -> Mono.fromCallable(() -> {
                            List<Float> queryVector = EmbeddingUtils.convertToFloatList(queryEmbedding);

                            int from = page * size;

                            Query knnQuery = Query.of(q -> q
                                    .knn(knn -> knn
                                            .field("embedding")
                                            .queryVector(queryVector)
                                            .k(1000)
                                            .numCandidates(2000)
                                    )
                            );

                            SearchRequest esReq = SearchRequest.of(s -> s
                                    .index(indexName)
                                    .query(knnQuery)
                                    .minScore(MIN_SIMILARITY)
                                    .from(from)
                                    .size(size)
                                    .sort(sort -> sort.field(f -> f.field("_score").order(SortOrder.Desc)))
                                    .collapse(c -> c.field("embedding"))
                            );

                            Optional<ElasticsearchClient> nativeClient = vectorStore.getNativeClient();
                            if (nativeClient.isEmpty()) {
                                throw new IllegalStateException("No native client available");
                            }
                            SearchResponse<MovieDocument> response = nativeClient.get().search(esReq, MovieDocument.class);

                            long totalItems = response.hits().total() != null
                                        ? response.hits().total().value()
                                        : 0;

                            List<MovieDocument> movies = response
                                    .hits()
                                    .hits()
                                    .stream()
                                    .map(Hit::source)
                                    .toList();

                            int totalPages = (int) Math.ceil((double) totalItems / size);

                            return new PageResponse<>(
                                    movies,
                                    page,
                                    size,
                                    totalItems,
                                    totalPages,
                                    page + 1 < totalPages,
                                    page > 0,
                                    page + 1 < totalPages ? page + 1 : null,
                                    page > 0 ? page - 1 : null
                            );
                        }));
    }

    @Override
    public Mono<PageResponse<MovieDocument>> searchSimilar(String query, int page, int size) {
        return searchByText(query, page, size);
    }
}
