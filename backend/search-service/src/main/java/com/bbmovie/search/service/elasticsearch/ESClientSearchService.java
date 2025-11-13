package com.bbmovie.search.service.elasticsearch;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.bbmovie.search.dto.PageResponse;
import com.bbmovie.search.dto.SearchCriteria;
import com.bbmovie.search.repository.search.SearchRepository;
import com.bbmovie.search.service.embedding.EmbeddingService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.bbmovie.search.dto.PageResponse.toPageResponse;
import static com.bbmovie.search.utils.EmbeddingUtils.convertToFloatList;

@Service
@Log4j2
@Primary
public class ESClientSearchService implements SearchService {

    private final SearchRepository searchRepository;
    private final EmbeddingService embeddingService;

    @Autowired
    public ESClientSearchService(SearchRepository searchRepository, EmbeddingService embeddingService) {
        this.searchRepository = searchRepository;
        this.embeddingService = embeddingService;
    }

    @Override
    public <T> Mono<PageResponse<T>> getAllMovies(int page, int size, int age, String region, Class<T> clazz) {
        return searchRepository.findAll(page, size, age, region, clazz)
                .map(response -> toPageResponse(response, page, size))
                .onErrorResume(ElasticsearchException.class, e -> {
                    log.error("ES all-movies search failed: {}", e.getMessage(), e);
                    return Mono.just(new PageResponse<>());
                });
    }

    @Override
    public <T> Mono<PageResponse<T>> searchSimilar(SearchCriteria criteria, Class<T> clazz) {
        return embeddingService.generateEmbedding(criteria.getQuery())
                .flatMap(vector -> {
                    List<Float> queryVector = convertToFloatList(vector);
                    return searchRepository.findSimilar(criteria, queryVector, clazz)
                            .map(response -> toPageResponse(response, criteria.getPage(), criteria.getSize()))
                            .onErrorResume(ElasticsearchException.class, e -> {
                                log.error("ES kNN search failed: {}", e.getMessage(), e);
                                return Mono.just(new PageResponse<>());
                            });
                });
    }
}
