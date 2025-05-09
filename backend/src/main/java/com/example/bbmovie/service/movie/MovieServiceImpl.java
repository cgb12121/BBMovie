package com.example.bbmovie.service.movie;

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
}
