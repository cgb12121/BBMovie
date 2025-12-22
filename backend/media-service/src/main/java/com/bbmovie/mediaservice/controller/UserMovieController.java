package com.bbmovie.mediaservice.controller;

import com.bbmovie.mediaservice.dto.MovieRequest;
import com.bbmovie.mediaservice.dto.MovieResponse;
import com.bbmovie.mediaservice.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/movies")
@RequiredArgsConstructor
public class UserMovieController {

    private final MovieService movieService;

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@RequestBody MovieRequest request) {
        MovieResponse response = movieService.createMovie(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{movieId}")
    public ResponseEntity<MovieResponse> getMovie(@PathVariable UUID movieId) {
        Optional<MovieResponse> movie = movieService.getMovieByMovieId(movieId);
        return movie.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}