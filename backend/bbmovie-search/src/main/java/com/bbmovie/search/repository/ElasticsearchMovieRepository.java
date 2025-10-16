package com.bbmovie.search.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.bbmovie.search.dto.SearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class ElasticsearchMovieRepository implements MovieRepository {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name}")
    private String indexName;

    @Value("${spring.ai.vectorstore.elasticsearch.embedding-field}")
    private String embeddingField;

    @Autowired
    public ElasticsearchMovieRepository(ElasticsearchClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    @Override
    public <T> Mono<SearchResponse<T>> findAll(int page, int size, int age, String region, Class<T> clazz) {
        return Mono.fromCallable(() -> {
            int from = page * size;

            BoolQuery boolQuery = buildFilter(age, region);
            boolean hasFilters = !boolQuery.must().isEmpty() || !boolQuery.filter().isEmpty();

            return elasticsearchClient.search(searchRequest -> searchRequest
                            .index(indexName)
                            .query(hasFilters ? q -> q.bool(boolQuery) : q -> q.matchAll(m -> m))  // Conditional to avoid empty bool
                            .from(from)
                            .size(size)
                            .source(src -> src
                                    .filter(f -> f.excludes(embeddingField))
                            ),
                    clazz);
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
    public <T> Mono<SearchResponse<T>> findSimilar(SearchCriteria criteria, List<Float> queryVector, Class<T> clazz) {
        return Mono.fromCallable(() -> {
            int from = criteria.getPage() * criteria.getSize();
            int k = Math.min(1000, criteria.getSize() * 10); //take more hits for manual pagination
            int numCandidates = Math.max(k * 2, criteria.getSize() * 20);

            return elasticsearchClient.search(s -> s
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
        });
    }
}
