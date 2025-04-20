package com.example.bbmovie.service.impl;

import com.example.bbmovie.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.bbmovie.dto.request.MovieCreateRequest;
import com.example.bbmovie.model.Movie;
import com.example.bbmovie.service.intf.MovieService;

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
