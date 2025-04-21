package com.example.bbmovie.service.elasticsearch.sample;

import com.example.bbmovie.model.*;
import com.example.bbmovie.model.elasticsearch.MovieDocument;
import com.example.bbmovie.repository.*;
import com.example.bbmovie.repository.elasticsearch.MovieSearchRepository;
import com.example.bbmovie.service.impl.HuggingFaceService;
import com.example.bbmovie.service.elasticsearch.MovieSearchService;
import com.example.bbmovie.service.elasticsearch.MovieVectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class SampleDataLoaderService {

    private final GenreRepository genreRepository;
    private final PersonRepository personRepository;
    private final MovieRepository movieRepository;
    private final MovieSearchRepository movieSearchRepository;
    private final MovieSearchService movieSearchService;
    private final MovieVectorSearchService movieVectorSearchService;
    private final HuggingFaceService huggingFaceService;

    @Transactional
    public void loadSampleData() {
        try {
            log.info("Starting to load sample data...");

            List<Genre> genres = Arrays.asList(
                SampleData.ACTION,
                SampleData.DRAMA,
                SampleData.COMEDY,
                SampleData.SCI_FI
            );
            genreRepository.saveAll(genres);
            log.info("Saved {} genres", genres.size());

            List<Person> actors = Arrays.asList(
                SampleData.TOM_HANKS,
                SampleData.LEONARDO_DICAPRIO,
                SampleData.MERYL_STREEP
            );
            personRepository.saveAll(actors);
            log.info("Saved {} actors", actors.size());

            List<Person> directors = Arrays.asList(
                SampleData.STEVEN_SPIELBERG,
                SampleData.CHRISTOPHER_NOLAN
            );
            personRepository.saveAll(directors);
            log.info("Saved {} directors", directors.size());

            Movie movie1 = SampleData.createSampleMovie1();
            Movie movie2 = SampleData.createSampleMovie2();
            List<Movie> movies = Arrays.asList(movie1, movie2);
            movieRepository.saveAll(movies);
            log.info("Saved {} movies to database", movies.size());

            MovieDocument movieDoc1 = SampleData.createSampleMovieDocument1();
            MovieDocument movieDoc2 = SampleData.createSampleMovieDocument2();
            movieSearchRepository.saveAll(Arrays.asList(movieDoc1, movieDoc2));
            log.info("Saved {} movie documents to Elasticsearch", 2);

            for (Movie movie : movies) {
                try {
                    movieVectorSearchService.indexMovieWithVector(movie);
                    log.info("Indexed movie with vector: {}", movie.getTitle());
                } catch (Exception e) {
                    log.error("Failed to index movie with vector: {}", movie.getTitle(), e);
                }
            }

            log.info("Sample data loading completed successfully");
        } catch (Exception e) {
            log.error("Error loading sample data", e);
            throw e;
        }
    }

    @Transactional
    public void cleanSampleData() {
        try {
            log.info("Starting to clean sample data...");

            movieRepository.deleteAll();
            personRepository.deleteAll();
            genreRepository.deleteAll();
            log.info("Cleaned up database");

            movieSearchRepository.deleteAll();
            log.info("Cleaned up Elasticsearch indices");

            log.info("Sample data cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error cleaning sample data", e);
            throw e;
        }
    }
} 