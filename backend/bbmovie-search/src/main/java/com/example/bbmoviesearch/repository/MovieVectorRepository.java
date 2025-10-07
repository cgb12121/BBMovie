package com.example.bbmoviesearch.repository;

import com.example.bbmoviesearch.dto.PageResponse;
import com.example.bbmoviesearch.entity.MovieDocument;
import reactor.core.publisher.Mono;

public interface MovieVectorRepository {
    Mono<Void> save(MovieDocument movie);

    Mono<PageResponse<MovieDocument>> searchByText(String query, int page, int size);

    Mono<PageResponse<MovieDocument>> searchSimilar(String query, int page, int size);
}
