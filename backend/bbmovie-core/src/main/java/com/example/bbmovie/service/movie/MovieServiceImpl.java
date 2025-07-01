package com.example.bbmovie.service.movie;

import com.example.bbmovie.entity.Movie;
import com.example.bbmovie.repository.MovieRepository;
import com.example.common.dtos.kafka.FileUploadEvent;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Log4j2(topic = "MovieService")
public class MovieServiceImpl implements MovieService {

     private final MovieRepository movieRepository;

     @Autowired
    public MovieServiceImpl(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
     @Transactional
     public void handleFileUpload(FileUploadEvent event) {
         Movie movie = movieRepository.findByTitle(event.getTitle())
                 .orElseGet(() -> createNewMovie(event));

        processFileType(event);

        processEntityType(event);

        processStorage(event);

        Movie saved = movieRepository.save(movie);
         log.info("Saved movie with ID: {}", saved.getId());
    }

    private static void processStorage(FileUploadEvent event) {
        switch (event.getStorage()) {
             case S3 -> {
                 // handle s3
             }
             case LOCAL -> {
                 // handle local
             }
             case CLOUDINARY -> {
                 // handle cloudinary
             }
             default -> throw new IllegalArgumentException("Unsupported storage type: " + event.getStorage());
         }
    }

    private static void processEntityType(FileUploadEvent event) {
        switch (event.getEntityType()) {
             case MOVIE -> {
                 // handle movie
             }
             case ACTOR -> {
                 // handle actor
             }
             case POSTER -> {
                 // handle poster
             }
             case DIRECTOR -> {
                 // handle director
             }
             case TRAILER -> {
                 // handle trailer
             }
             default -> throw new IllegalArgumentException("Unsupported entity type: " + event.getEntityType());
         }
    }

    private static void processFileType(FileUploadEvent event) {
        switch (event.getFileType()) {
            case IMAGE -> {
                // handle image
            }
            case VIDEO -> {
                // handle video
            }
            default -> throw new IllegalArgumentException("Unsupported file type: " + event.getFileType());
        }
    }

    private static Movie createNewMovie(FileUploadEvent event) {
        log.warn("Movie with title '{}' not found. Creating new movie.", event.getTitle());
        Movie newMovie = new Movie();
        newMovie.setTitle(event.getTitle());
        newMovie.setCreatedDate(LocalDateTime.now());
        newMovie.setDescription("Uploaded via event");
        newMovie.setReleaseDate(LocalDate.now());
        return newMovie;
    }
}
