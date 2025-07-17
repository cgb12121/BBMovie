package com.example.bbmoviestream.repository;

import com.example.bbmoviestream.entity.Movie;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface MovieRepository extends ReactiveCrudRepository<Movie, String> {
    Mono<Movie> findByTitle(String title);

    Mono<Object> findByFilename(String filename);
}