package com.bbmovie.search.repository;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.bbmovie.search.dto.SearchCriteria;
import reactor.core.publisher.Mono;

import java.util.List;

public interface MovieRepository {
    <T> Mono<SearchResponse<T>> findAll(int page, int size, int age, String region, Class<T> clazz);
    <T> Mono<SearchResponse<T>> findSimilar(SearchCriteria criteria, List<Float> queryVector, Class<T> clazz);
}
