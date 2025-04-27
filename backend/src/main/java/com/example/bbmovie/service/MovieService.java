package com.example.bbmovie.service;

import com.example.bbmovie.dto.request.MovieCreateRequest;
import com.example.bbmovie.entity.Movie;

import java.util.List;

public interface MovieService {
    Movie createMovie(MovieCreateRequest request, String posterUrl);

    List<Movie> getAllMovies();
}
