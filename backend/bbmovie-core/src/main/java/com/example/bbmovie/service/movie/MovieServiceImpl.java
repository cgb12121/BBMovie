package com.example.bbmovie.service.movie;

import com.example.bbmovie.entity.Movie;
import com.example.bbmovie.repository.MovieRepository;
import com.example.common.dtos.kafka.FileUploadEvent;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static com.example.common.enums.Storage.*;

@Log4j2
@Service
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    @Autowired
    public MovieServiceImpl(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    @Transactional
    public void handleFileUpload(FileUploadEvent event) {
        Movie movie = movieRepository.findByTitle(event.getTitle()).orElseGet(() -> createNewMovie(event));
        processEntityType(movie, event);
        processStorage(movie, event);
        Movie saved = movieRepository.save(movie);
        log.info("Saved movie with ID: {}", saved.getId());
    }

    private void processStorage(Movie movie, FileUploadEvent event) {
        switch (event.getStorage()) {
            case S3 -> movie.setStorage(S3.name());
            case LOCAL -> movie.setStorage(LOCAL.name());
            case CLOUDINARY -> movie.setStorage(CLOUDINARY.name());
            default -> throw new IllegalArgumentException("Unsupported storage type: " + event.getStorage());
        }
    }

    private void processEntityType(Movie movie, FileUploadEvent event) {
        switch (event.getEntityType()) {
            case MOVIE -> movie.setVideoUrl(event.getUrl());
            case POSTER -> movie.setPosterUrl(event.getUrl());
            case TRAILER -> movie.setTrailerUrl(event.getUrl());
            default -> throw new IllegalArgumentException("Unsupported entity type: " + event.getEntityType());
        }
    }

    private static Movie createNewMovie(FileUploadEvent event) {
        log.warn("Movie with title '{}' not found. Creating new movie.", event.getTitle());
        Movie newMovie = new Movie();
        newMovie.setTitle(event.getTitle());
        newMovie.setCreatedDate(event.getTimestamp());
        newMovie.setDescription("Uploaded via event");
        newMovie.setReleaseDate(LocalDate.now());
        return newMovie;
    }
}
