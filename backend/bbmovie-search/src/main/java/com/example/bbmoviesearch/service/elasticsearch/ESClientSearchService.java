package com.example.bbmoviesearch.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOrder;
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

    private static final String EMBEDDING_FIELD = "embedding";

    @Autowired
    public ESClientSearchService(ElasticsearchClient elasticsearchClient, EmbeddingService embeddingService) {
        this.elasticsearchClient = elasticsearchClient;
        this.embeddingService = embeddingService;
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
        .doOnError(log::error);
    }

    //TODO: fix error that pagination only work for page=0
    @Override
    public <T> Mono<PageResponse<T>> searchSimilar(SearchCriteria criteria, Class<T> clazz) {
        return embeddingService.generateEmbedding(criteria.getQuery())
                .flatMap(vector -> Mono.fromCallable(() -> {
                    List<Float> queryVector = convertToFloatList(vector);
                    log.info("Generated embedding for query: {}", vector.length);
                    log.info("Embedding: {}", queryVector.size());
                    int from = criteria.getPage() * criteria.getSize();

                    SearchResponse<T> response = elasticsearchClient.search(s -> s
                                    .index(indexName)
                                    .knn(knn -> knn
                                            .field(EMBEDDING_FIELD)
                                            .queryVector(queryVector)
                                            .k(criteria.getSize())
                                            .numCandidates(criteria.getSize() * 2)
                                    )
//                                    .sort(sort -> t.field(
//                                            f -> {
//                                                //TODO: create sort properly
//                                                FieldSort.Builder sort = new FieldSort.Builder();
//                                                        sort.field("rating").order(SortOrder.Desc);
//                                                        sort.field("releaseDate").order(SortOrder.Desc);
//                                                return sort;
//                                            }
//                                    ))
                                    .from(from)
                                    .size(criteria.getSize())
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
                    }).doOnError(log::error)
                );
    }
}