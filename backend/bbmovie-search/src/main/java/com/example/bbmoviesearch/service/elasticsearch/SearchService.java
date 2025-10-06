package com.example.bbmoviesearch.service.elasticsearch;

import com.example.bbmoviesearch.dto.PageResponse;
import reactor.core.publisher.Mono;

public interface SearchService {
    <T> Mono<PageResponse<T>> getAllMovies(int page, int size, int age, String region, Class<T> clazz);
    <T> Mono<PageResponse<T>> searchSimilarMovies(SearchCriteria criteria, Class<T> clazz);
}