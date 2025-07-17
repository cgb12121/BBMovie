package com.example.bbmoviestream.service;

import com.example.bbmoviestream.entity.Movie;
import com.example.common.enums.Storage;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LocalVideoStreamingStrategy implements VideoStreamingStrategy {

    private final ResourceLoader resourceLoader;

    public LocalVideoStreamingStrategy(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public Storage getStorageType() {
        return Storage.LOCAL;
    }

    @Override
    public Mono<Resource> loadVideoResource(Movie movie) {
        return Mono.fromCallable(() -> {
            Path path = Paths.get(movie.getFilePath());
            if (!Files.exists(path)) {
                throw new FileNotFoundException("Local file not found: " + movie.getFilePath());
            }
            return resourceLoader.getResource("file:" + path.toAbsolutePath());
        });
    }

    @Override
    public Mono<Resource> loadPartialResource(Movie movie, long start, long length) {
        return loadVideoResource(movie).flatMap(resource -> Mono.fromCallable(() -> {
            InputStream is = resource.getInputStream();
            if (is.skip(start) != start) {
                throw new IOException("Unable to skip to start");
            }
            return new InputStreamResource(new LimitedInputStream(is, length));
        }));
    }

    @Override
    public Mono<String> generateHlsPlaylist(Movie movie) {
        return Mono.fromCallable(() -> {
            if (movie.getHlsPlaylistPath() == null) {
                throw new IllegalStateException("No HLS path set");
            }
            Path playlistPath = Paths.get(movie.getHlsPlaylistPath());
            if (Files.exists(playlistPath)) {
                return Files.readString(playlistPath);
            }
            throw new FileNotFoundException("HLS playlist not found");
        });
    }
}
