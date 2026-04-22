package com.bbmovie.search.repository.esclient;

import com.bbmovie.search.entity.MovieDocument;
import reactor.core.publisher.Mono;

public interface MovieMetadataRepository {
    Mono<MovieDocument> findById(String id);
    Mono<MovieDocument> save(MovieDocument document);
}
