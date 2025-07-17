package com.example.bbmoviestream.service;

import com.example.bbmoviestream.entity.Movie;
import com.example.common.enums.Storage;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

public interface VideoStreamingStrategy {
    Storage getStorageType();
    Mono<Resource> loadVideoResource(Movie movie);
    Mono<Resource> loadPartialResource(Movie movie, long start, long length);
    Mono<String> generateHlsPlaylist(Movie movie);
}
