package com.example.bbmovie.service.movie;

import com.example.bbmovie.dto.kafka.consumer.FileUploadEvent;
import com.example.bbmovie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

     private final MovieRepository movieRepository;

     public void updatePoster(Long id) {
          movieRepository.updatePoster(id, null, null);
     }

     @Override
     public void handleFileUpload(FileUploadEvent event) {
        movieRepository.findById(event.getMovieId()).ifPresent(movie -> {
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
            }
            movieRepository.save(movie);
        });
    }

     //TODO: implemet HLS for video
}
