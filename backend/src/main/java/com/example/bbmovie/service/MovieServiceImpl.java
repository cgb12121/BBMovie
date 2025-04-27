package com.example.bbmovie.service;

import com.example.bbmovie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.bbmovie.dto.request.MovieCreateRequest;
import com.example.bbmovie.entity.Movie;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieServiceImpl implements MovieService {

     private final MovieRepository movieRepository;

     @Override
     public Movie createMovie(MovieCreateRequest request, String posterUrl) {
          return null;
     };

     @Override
     public List<Movie> getAllMovies() {
          return movieRepository.findAll();
     }
}
