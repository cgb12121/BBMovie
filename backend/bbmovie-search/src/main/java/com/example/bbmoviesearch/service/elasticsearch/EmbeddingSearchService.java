package com.example.bbmoviesearch.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.bbmoviesearch.dto.PageResponse;
import com.example.bbmoviesearch.service.embedding.LocalEmbeddingService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
public class EmbeddingSearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final LocalEmbeddingService localEmbeddingService;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    @Autowired
    public EmbeddingSearchService(
            ElasticsearchClient elasticsearchClient,
            LocalEmbeddingService localEmbeddingService
    ) {
        this.elasticsearchClient = elasticsearchClient;
        this.localEmbeddingService = localEmbeddingService;
    }

    public <T> Mono<PageResponse<T>> getAllMovies(int page, int size, int age, String region, Class<T> clazz) {
        return Mono.fromCallable(() -> {
            int from = page * size;

            SearchResponse<T> response = elasticsearchClient.search(searchRequest -> searchRequest
                            .index(indexName)
                            .query(q -> q.matchAll(m -> m))
                            .from(from)
                            .size(size)
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.range(r -> r
                                            .term(t -> t
                                                    .field("ageRating")
                                                    .lte(String.valueOf(age))
                                            )
                                    ))
                                    .filter(f -> f
                                            .term(t -> t
                                                    .field("region")
                                                    .value(region))
                                    ) // region filter
                            ))
                            .source(src -> src
                                    .filter(f -> f
                                            .excludes(QueryEmbeddingField.EMBEDDING_FIELD)
                                    )
                            )
                            .size(1000),
                    clazz);

            long totalItems = response.hits().total() != null
                    ? response.hits().total().value()
                    : 0;
            int totalPages = (int) Math.ceil((double) totalItems / size);

            List<T> items = response
                    .hits()
                    .hits()
                    .stream()
                    .map(Hit::source)
                    .toList();

            return new PageResponse<>(
                    items,
                    page,
                    size,
                    totalItems,
                    totalPages,
                    page + 1 < totalPages,
                    page > 0,
                    page + 1 < totalPages ? page + 1 : null,
                    page > 0 ? page - 1 : null
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public <T> Mono<PageResponse<T>> searchSimilarMovies(String query, int page, int size, int age, String region, Class<T> clazz) {
        return Mono.fromCallable(() -> {
            float[] vector = localEmbeddingService.generateEmbedding(query);
            List<Float> queryVector = convertToFloatList(vector);
            int from = page * size;

            SearchResponse<T> response = elasticsearchClient.search(searchRequest -> searchRequest
                            .index(indexName)
                            .knn(knn -> knn
                                    .field(QueryEmbeddingField.EMBEDDING_FIELD)
                                    .queryVector(queryVector)
                                    .k(size)
                                    .numCandidates(size * 2))
                            .from(from)
                            .size(size)
                            .query(q -> q.bool(b -> b
                                    .must(m -> m.range(r -> r
                                            .term(t -> t
                                                    .field("ageRating")
                                                    .lte(String.valueOf(age))
                                            )
                                    ))
                                    .filter(f -> f
                                            .term(t -> t
                                                .field("region")
                                                .value(region))
                                    ) // region filter
                            ))
                            .source(src -> src
                                    .filter(f -> f
                                            .excludes(QueryEmbeddingField.EMBEDDING_FIELD)
                                    )
                            ),
                    clazz);

            long totalItems = response.hits().total() != null ? response.hits().total().value() : 0;
            int totalPages = (int) Math.ceil((double) totalItems / size);

            List<T> items = response.hits().hits()
                    .stream()
                    .map(Hit::source)
                    .toList();

            return new PageResponse<>(
                    items,
                    page,
                    size,
                    totalItems,
                    totalPages,
                    page + 1 < totalPages,
                    page > 0,
                    page + 1 < totalPages ? page + 1 : null,
                    page > 0 ? page - 1 : null
            );
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private List<Float> convertToFloatList(float[] floats) {
        List<Float> list = new ArrayList<>(floats.length);
        for (float f : floats) list.add(f);
        return list;
    }
}
