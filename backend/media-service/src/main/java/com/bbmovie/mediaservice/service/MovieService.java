package com.bbmovie.mediaservice.service;

import com.bbmovie.mediaservice.dto.MovieRequest;
import com.bbmovie.mediaservice.dto.MovieResponse;
import com.bbmovie.mediaservice.entity.Movie;
import com.bbmovie.mediaservice.entity.MovieStatus;
import com.bbmovie.mediaservice.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieResponse createMovie(MovieRequest request) {
        // Generate a unique movie ID if not provided
        UUID movieId = request.getMovieId() != null ? request.getMovieId() : UUID.randomUUID();

        Movie movie = Movie.builder()
                .movieId(movieId)
                .title(request.getTitle())
                .description(request.getDescription())
                .director(request.getDirector())
                .cast(request.getCast())
                .duration(request.getDuration())
                .genre(request.getGenre())
                .releaseDate(request.getReleaseDate())
                .posterUrl(request.getPosterUrl())
                .trailerUrl(request.getTrailerUrl())
                .fileId(request.getFileId())
                .status(request.getStatus() != null ? request.getStatus() : MovieStatus.DRAFT)
                .build();

        Movie savedMovie = movieRepository.save(movie);
        log.info("Created movie with ID: {} and status: {}", savedMovie.getMovieId(), savedMovie.getStatus());
        return mapToResponse(savedMovie);
    }

    public Optional<MovieResponse> getMovieByMovieId(UUID movieId) {
        return movieRepository.findByMovieId(movieId)
                .map(this::mapToResponse);
    }

    public Optional<MovieResponse> getMovieById(UUID id) {
        return movieRepository.findById(id)
                .map(this::mapToResponse);
    }

    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<MovieResponse> getMoviesByStatus(MovieStatus status) {
        return movieRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<MovieResponse> searchMoviesByTitle(String title) {
        return movieRepository.findByTitleContainingIgnoreCase(title)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Optional<MovieResponse> updateMovie(UUID movieId, MovieRequest request) {
        return movieRepository.findByMovieId(movieId)
                .map(movie -> {
                    movie.setTitle(request.getTitle());
                    movie.setDescription(request.getDescription());
                    movie.setDirector(request.getDirector());
                    movie.setCast(request.getCast());
                    movie.setDuration(request.getDuration());
                    movie.setGenre(request.getGenre());
                    movie.setReleaseDate(request.getReleaseDate());
                    movie.setPosterUrl(request.getPosterUrl());
                    movie.setTrailerUrl(request.getTrailerUrl());
                    movie.setFileId(request.getFileId());
                    movie.setStatus(request.getStatus());
                    movie.setUpdatedAt(LocalDateTime.now());
                    
                    Movie updatedMovie = movieRepository.save(movie);
                    log.info("Updated movie with ID: {}", updatedMovie.getMovieId());
                    return mapToResponse(updatedMovie);
                });
    }

    public boolean deleteMovie(UUID movieId) {
        Optional<Movie> movie = movieRepository.findByMovieId(movieId);
        if (movie.isPresent()) {
            movie.get().setStatus(MovieStatus.DELETED);
            movie.get().setUpdatedAt(LocalDateTime.now());
            movieRepository.save(movie.get());
            log.info("Soft deleted movie with ID: {}", movieId);
            return true;
        }
        return false;
    }

    public Optional<MovieResponse> linkFileToMovie(UUID movieId, String fileId) {
        return movieRepository.findByMovieId(movieId)
                .map(movie -> {
                    movie.setFileId(fileId);
                    // Only update status to PROCESSING if it's currently DRAFT
                    if (movie.getStatus() == MovieStatus.DRAFT) {
                        movie.setStatus(MovieStatus.PROCESSING); // Move to processing when file is linked
                    }
                    movie.setUpdatedAt(LocalDateTime.now());

                    Movie updatedMovie = movieRepository.save(movie);
                    log.info("Linked file {} to movie {} and updated status to {}", fileId, movieId, updatedMovie.getStatus());
                    return mapToResponse(updatedMovie);
                });
    }

    public Optional<MovieResponse> updateMovieStatus(UUID movieId, MovieStatus status) {
        return movieRepository.findByMovieId(movieId)
                .map(movie -> {
                    MovieStatus oldStatus = movie.getStatus();
                    movie.setStatus(status);
                    movie.setUpdatedAt(LocalDateTime.now());

                    Movie updatedMovie = movieRepository.save(movie);
                    log.info("Updated movie {} status from {} to {}", movieId, oldStatus, status);

                    // If the status changed to PUBLISHED, we should publish a MoviePublishedEvent
                    if (status == MovieStatus.PUBLISHED && oldStatus != MovieStatus.PUBLISHED) {
                        log.info("Movie {} is now PUBLISHED, should publish MoviePublishedEvent", movieId);
                        // In a real implementation, we would publish this event to NATS
                        // For now, we'll just log it - the actual publishing would be handled by a separate service
                    }

                    return mapToResponse(updatedMovie);
                });
    }

    public Optional<MovieResponse> updateMovieFilePath(UUID movieId, String filePath) {
        return movieRepository.findByMovieId(movieId)
                .map(movie -> {
                    movie.setFilePath(filePath);
                    movie.setUpdatedAt(LocalDateTime.now());

                    Movie updatedMovie = movieRepository.save(movie);
                    log.info("Updated movie {} file path to {}", movieId, filePath);
                    return mapToResponse(updatedMovie);
                });
    }

    public Optional<MovieResponse> updateMovieStatusByFileId(String fileId, MovieStatus status) {
        return movieRepository.findByFileId(fileId)
                .map(movie -> {
                    movie.setStatus(status);
                    movie.setUpdatedAt(LocalDateTime.now());

                    Movie updatedMovie = movieRepository.save(movie);
                    log.info("Updated movie {} (fileId: {}) status to {}", updatedMovie.getMovieId(), fileId, status);
                    return mapToResponse(updatedMovie);
                });
    }

    private MovieResponse mapToResponse(Movie movie) {
        return MovieResponse.builder()
                .id(movie.getMovieId())
                .movieId(movie.getMovieId())
                .title(movie.getTitle())
                .description(movie.getDescription())
                .director(movie.getDirector())
                .cast(movie.getCast())
                .duration(movie.getDuration())
                .genre(movie.getGenre())
                .releaseDate(movie.getReleaseDate())
                .posterUrl(movie.getPosterUrl())
                .trailerUrl(movie.getTrailerUrl())
                .fileId(movie.getFileId())
                .status(movie.getStatus())
                .filePath(movie.getFilePath())
                .createdAt(movie.getCreatedAt())
                .updatedAt(movie.getUpdatedAt())
                .build();
    }
}