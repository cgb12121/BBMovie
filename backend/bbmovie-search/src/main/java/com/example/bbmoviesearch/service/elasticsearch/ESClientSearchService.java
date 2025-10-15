package com.example.bbmoviesearch.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.bbmoviesearch.dto.PageResponse;
import com.example.bbmoviesearch.dto.SearchCriteria;
import com.example.bbmoviesearch.service.embedding.EmbeddingService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.example.bbmoviesearch.utils.EmbeddingUtils.convertToFloatList;

@Service
@Log4j2
@Primary
public class ESClientSearchService implements SearchService {

    private final ElasticsearchClient elasticsearchClient;
    private final EmbeddingService embeddingService;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    @Value("${spring.ai.vectorstore.elasticsearch.embedding-field}")
    private String embeddingField;

    @Autowired
    public ESClientSearchService(ElasticsearchClient elasticsearchClient, EmbeddingService embeddingService) {
        this.elasticsearchClient = elasticsearchClient;
        this.embeddingService = embeddingService;
    }

    @Override
    public <T> Mono<PageResponse<T>> getAllMovies(int page, int size, int age, String region, Class<T> clazz) {
        return Mono.fromCallable(() -> {
                    int from = page * size;

                    BoolQuery boolQuery = buildFilter(age, region);
                    boolean hasFilters = !boolQuery.must().isEmpty() || !boolQuery.filter().isEmpty();

                    SearchResponse<T> response = elasticsearchClient.search(searchRequest -> searchRequest
                                    .index(indexName)
                                    .query(hasFilters ? q -> q.bool(boolQuery) : q -> q.matchAll(m -> m))  // Conditional to avoid empty bool
                                    .from(from)
                                    .size(size)  // FIXED: Only this size—no override
                                    .source(src -> src
                                            .filter(f -> f.excludes(embeddingField))
                                    ),
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
                .onErrorResume(ElasticsearchException.class, e -> {
                    log.error("ES all-movies search failed: {}", e.getMessage(), e);
                    return Mono.just(new PageResponse<>(
                            List.of(),
                            page,
                            size,
                            0L,
                            1,
                            false,
                            false,
                            null,
                            null
                    ));
                });
    }

    private BoolQuery buildFilter(int age, String region) {
        BoolQuery.Builder boolBuilder = new BoolQuery.Builder();
        if (age > 0) {
            boolBuilder.must(m -> m.range(r -> r.term(t -> t
                            .field("ageRating")
                            .lte(String.valueOf(age)))
            ));
        }
        if (region != null && !region.isEmpty()) {
            boolBuilder.filter(f -> f.term(t -> t
                    .field("region")
                    .value(region)
            ));
        }
        return boolBuilder.build();
    }

    @Override
    public <T> Mono<PageResponse<T>> searchSimilar(SearchCriteria criteria, Class<T> clazz) {
        return embeddingService.generateEmbedding(criteria.getQuery())
                .flatMap(vector -> Mono.fromCallable(() -> {
                    List<Float> queryVector = convertToFloatList(vector);

                    int from = criteria.getPage() * criteria.getSize();
                    int k = Math.min(1000, criteria.getSize() * 10); //take more hits for manual pagination
                    int numCandidates = Math.max(k * 2, criteria.getSize() * 20);

                    SearchResponse<T> response = elasticsearchClient.search(s -> s
                                    .index(indexName)
                                    .knn(knn -> {
                                        knn.field(embeddingField)
                                                .queryVector(queryVector)
                                                .k(k)
                                                .numCandidates(numCandidates);

                                        if (criteria.getType() != null) {
                                            knn.filter(f -> f.term(t -> t
                                                    .field("type")
                                                    .value(criteria.getType().get())
                                            ));
                                        }
                                        return knn;
                                    })
                                    .sort(sort -> sort.field(
                                            f -> {
                                                if (criteria.getFilterBy() != null ) {
                                                    // just apply only 1 option for FilterBy
                                                    switch (criteria.getFilterBy()) {
                                                        case newest -> f.field("releaseDate");
                                                        case rating -> f.field("rating");
                                                        case most_view -> f.field("viewCount");
                                                    }
                                                }

                                                if (criteria.getSortOrder() != null) {
                                                    f.order(criteria.getSortOrder().get());
                                                }
                                                return f;
                                            }
                                    ))
                                    .from(from)
                                    .size(criteria.getSize())
                                    .source(src -> src.filter(f -> f.excludes(embeddingField))),
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
                    .onErrorResume(ElasticsearchException.class, e -> {
                        log.error("ES kNN search failed: {}", e.getMessage(), e);
                        return Mono.just(new PageResponse<>());
                    }));
    }
}