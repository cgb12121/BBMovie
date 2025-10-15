package com.bbmovie.search.repository;

import com.bbmovie.search.dto.PageResponse;
import com.bbmovie.search.entity.MovieDocument;
import reactor.core.publisher.Mono;

public interface MovieVectorRepository {
    Mono<Void> save(MovieDocument movie);

    Mono<PageResponse<MovieDocument>> searchByText(String query, int page, int size);

    Mono<PageResponse<MovieDocument>> searchSimilar(String query, int page, int size);
}
