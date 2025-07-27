package com.example.bbmoviesearch.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
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

    public Mono<List<Object>> getAllMovies() {
        return Mono.fromCallable(() -> {
            SearchResponse<Object> response = elasticsearchClient.search(searchRequest -> searchRequest
                            .index(indexName)
                            .query(q -> q.matchAll(m -> m))
                            .source(src -> src
                                    .filter(f -> f
                                            .excludes(QueryEmbeddingField.EMBEDDING_FIELD)
                                    )
                            )
                            .size(1000),
                    Object.class);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<List<Object>> searchSimilarMovies(String query, int limit) {
        return Mono.fromCallable(() -> {
            float[] vector = localEmbeddingService.generateEmbedding(query);
            List<Float> queryVector = convertToFloatList(vector);

            SearchResponse<Object> response = elasticsearchClient.search(searchRequest -> searchRequest
                            .index(indexName)
                            .knn(knn -> knn
                                    .field(QueryEmbeddingField.EMBEDDING_FIELD)
                                    .queryVector(queryVector)
                                    .k(limit)
                                    .numCandidates(limit * 2))
                            .source(src -> src
                                    .filter(f -> f
                                            .excludes(QueryEmbeddingField.EMBEDDING_FIELD)
                                    )
                            ),
                    Object.class);

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private List<Float> convertToFloatList(float[] floats) {
        List<Float> list = new ArrayList<>(floats.length);
        for (float f : floats) list.add(f);
        return list;
    }
}
