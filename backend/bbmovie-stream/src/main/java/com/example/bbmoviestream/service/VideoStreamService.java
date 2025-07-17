package com.example.bbmoviestream.service;

import com.example.bbmoviestream.entity.Movie;
import com.example.bbmoviestream.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class VideoStreamService {

    private final MovieRepository movieRepository;
    private final VideoStreamingStrategyFactory strategyFactory;

    public VideoStreamService(MovieRepository movieRepository, VideoStreamingStrategyFactory strategyFactory) {
        this.movieRepository = movieRepository;
        this.strategyFactory = strategyFactory;
    }

    public Mono<Movie> getMovie(String filename) {
        return movieRepository.findByFilename(filename)
                .switchIfEmpty(Mono.error(new ChangeSetPersister.NotFoundException()));
    }

    public Mono<Resource> getVideoResource(String filename) {
        return getMovie(filename)
                .flatMap(movie -> strategyFactory.getStrategy(movie.getStorage()).loadVideoResource(movie));
    }

    public Mono<Resource> getPartialResource(String filename, long start, long length) {
        return getMovie(filename)
                .flatMap(movie -> strategyFactory.getStrategy(movie.getStorage()).loadPartialResource(movie, start, length));
    }

    public Mono<String> generateHlsPlaylist(String filename) {
        return getMovie(filename)
                .flatMap(movie -> strategyFactory.getStrategy(movie.getStorage()).generateHlsPlaylist(movie));
    }
}
