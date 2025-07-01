package com.example.bbmovie.service.movie;

import com.example.bbmovie.repository.MovieRepository;
import com.example.common.dtos.kafka.FileUploadEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

     private final MovieRepository movieRepository;

     @Override
     public void handleFileUpload(FileUploadEvent event) {
        movieRepository.findByTitle(event.getTitle()).ifPresent(movie -> {
            switch (event.getFileType()) {
                case "POSTER" -> {
                    movie.setPosterUrl(event.getUrl());
                    movie.setPosterPublicId(event.getPublicId());
                }
                case "VIDEO" -> {
                    movie.setVideoUrl(event.getUrl());
                    movie.setVideoPublicId(event.getPublicId());
                }
                case "TRAILER" -> {
                    movie.setTrailerUrl(event.getUrl());
                    movie.setTrailerPublicId(event.getPublicId());
                }
                default -> throw new IllegalArgumentException("Unsupported file type: " + event.getFileType());
            }
            movieRepository.save(movie);
        });
    }

     //TODO: implemet HLS for video
}
