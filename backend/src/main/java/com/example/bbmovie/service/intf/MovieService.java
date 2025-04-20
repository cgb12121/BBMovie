package com.example.bbmovie.service.intf;

import com.example.bbmovie.dto.request.MovieCreateRequest;
import com.example.bbmovie.model.Movie;

import java.util.List;

public interface MovieService {
    Movie createMovie(MovieCreateRequest request, String posterUrl);

    List<Movie> getAllMovies();
}
