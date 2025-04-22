package com.example.bbmovie.service.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.example.bbmovie.model.Movie;
import com.example.bbmovie.model.elasticsearch.MovieDocument;
import com.example.bbmovie.model.elasticsearch.MovieVectorDocument;
import com.example.bbmovie.repository.elasticsearch.MovieSearchRepository;

import com.example.bbmovie.service.HuggingFaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service("elastic")
@Log4j2
@RequiredArgsConstructor
public class MovieSearchService {

   private final MovieSearchRepository movieSearchRepository;
   private final ElasticsearchClient elasticsearchClient;
   private final HuggingFaceService huggingFaceService;

   public List<MovieDocument> searchMovies(String query) throws IOException {
       SearchResponse<MovieDocument> response = elasticsearchClient.search(s -> s
                       .index("movies")
                       .query(q -> q
                               .multiMatch(m -> m
                                       .query(query)
                                       .fields(Arrays.asList("title^2", "description", "categories"))
                               )
                       ),
               MovieDocument.class
       );

       return response.hits().hits().stream()
               .map(Hit::source)
               .collect(Collectors.toList());
   }

   public void indexMovieWithVector(Movie movie) throws IOException {
       String content = movie.getTitle() + " " + movie.getDescription();
       float[] contentVector = huggingFaceService.generateEmbedding(content);

       MovieVectorDocument document = MovieVectorDocument.builder()
               .id(movie.getId().toString())
               .title(movie.getTitle())
               .description(movie.getDescription())
               .contentVector(contentVector)
               .rating(movie.getRating())
               .categories(movie.getCategories().stream().toList())
               .posterUrl(movie.getPosterUrl())
               .releaseDate(movie.getReleaseDate().atStartOfDay())
               .build();

       elasticsearchClient.index(i -> i
               .index(movie.getId().toString())
               .id(document.getId())
               .document(document)
       );
   }

   public void reindexAll(List<Movie> movies) {
       movieSearchRepository.deleteAll();
       movies.forEach(movie -> {
           try {
               indexMovieWithVector(movie);
           } catch (IOException e) {
               log.error("Something went wrong when indexing movies: {}", e.getMessage());
           }
       });
   }
}