package com.example.bbmoviesearch.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.bbmoviesearch.dto.PageResponse;
import com.example.bbmoviesearch.service.embedding.DjLEmbeddingService;
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
public class SimilaritySearchService implements SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final DjLEmbeddingService djLEmbeddingService;
    private final ElasticQueryBuilder queryBuilder;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    private static final String EMBEDDING_FIELD = "contentVector";

    @Autowired
    public SimilaritySearchService(ElasticsearchClient elasticsearchClient, DjLEmbeddingService djLEmbeddingService, ElasticQueryBuilder queryBuilder) {
        this.elasticsearchClient = elasticsearchClient;
        this.djLEmbeddingService = djLEmbeddingService;
        this.queryBuilder = queryBuilder;
    }

    @Override
    public <T> Mono<PageResponse<T>> getAllMovies(int page, int size, int age, String region, Class<T> clazz) {
        return Mono.fromCallable(() -> {
            int from = page * size;

            SearchResponse<T> response = elasticsearchClient.search(searchRequest -> searchRequest
                            .index(indexName)
                            .query(q -> q.matchAll(m -> m))
                            .from(from)
                            .size(size)
                            .source(src -> src
                                    .filter(f -> f
                                            .excludes(EMBEDDING_FIELD)
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
        })
        .doOnError(log::error)
        .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public <T> Mono<PageResponse<T>> searchSimilarMovies(SearchCriteria criteria, Class<T> clazz) {
        return djLEmbeddingService.generateEmbedding(criteria.getQuery())
                .flatMap(vector -> Mono.fromCallable(() -> {
                                    List<Float> queryVector = convertToFloatList(vector);
                                    int from = criteria.getPage() * criteria.getSize();

                                    BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
                                    queryBuilder.applyCriteria(boolBuilder, criteria);

                                    SearchResponse<T> response = elasticsearchClient.search(s -> s
                                                    .index(indexName)
                                                    .knn(knn -> knn
                                                            .field(EMBEDDING_FIELD)
                                                            .queryVector(queryVector)
                                                            .k(criteria.getSize())
                                                            .numCandidates(criteria.getSize() * 2))
                                                    .query(q -> q.bool(boolBuilder.build()))
                                                    .from(from)
                                                    .size(criteria.getSize())
                                                    .sort(queryBuilder.buildSort(criteria))
                                                    .source(src -> src.filter(f -> f.excludes(EMBEDDING_FIELD))),
                                            clazz);

                                    long totalItems = response.hits().total() != null
                                            ? response.hits().total().value()
                                            : 0;
                                    int totalPages = (int) Math.ceil((double) totalItems / criteria.getSize());

                                    List<T> items = response.hits().hits()
                                            .stream()
                                            .map(Hit::source)
                                            .toList();

                                    return new PageResponse<>(
                                            items,
                                            criteria.getPage(),
                                            criteria.getSize(),
                                            totalItems,
                                            totalPages,
                                            criteria.getPage() + 1 < totalPages,
                                            criteria.getPage() > 0,
                                            criteria.getPage() + 1 < totalPages ? criteria.getPage() + 1 : null,
                                            criteria.getPage() > 0 ? criteria.getPage() - 1 : null
                                    );
                                })
                                .subscribeOn(Schedulers.boundedElastic())
                );
    }


    private List<Float> convertToFloatList(float[] floats) {
        List<Float> list = new ArrayList<>(floats.length);
        for (float f : floats) list.add(f);
        return list;
    }
}