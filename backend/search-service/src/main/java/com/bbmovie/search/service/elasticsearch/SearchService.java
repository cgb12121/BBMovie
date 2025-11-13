package com.bbmovie.search.service.elasticsearch;

import com.bbmovie.search.dto.PageResponse;
import com.bbmovie.search.dto.SearchCriteria;
import reactor.core.publisher.Mono;

public interface SearchService {
    <T> Mono<PageResponse<T>> getAllMovies(int page, int size, int age, String region, Class<T> clazz);
    <T> Mono<PageResponse<T>> searchSimilar(SearchCriteria criteria, Class<T> clazz);
}
