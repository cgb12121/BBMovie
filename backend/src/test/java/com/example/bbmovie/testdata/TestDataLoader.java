package com.example.bbmovie.testdata;

import com.example.bbmovie.model.*;
import com.example.bbmovie.model.elasticsearch.MovieDocument;
import com.example.bbmovie.model.elasticsearch.MovieVectorDocument;
import com.example.bbmovie.repository.*;
import com.example.bbmovie.repository.elasticsearch.MovieSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TestDataLoader {

    private final GenreRepository genreRepository;
    private final PersonRepository personRepository;
    private final MovieRepository movieRepository;
    private final MovieSearchRepository movieSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Transactional
    public void loadTestData() {
        List<Genre> genres = Arrays.asList(
            TestData.ACTION,
            TestData.DRAMA,
            TestData.COMEDY,
            TestData.SCI_FI
        );
        genreRepository.saveAll(genres);

        List<Person> actors = Arrays.asList(
            TestData.TOM_HANKS,
            TestData.LEONARDO_DICAPRIO,
            TestData.MERYL_STREEP
        );
        personRepository.saveAll(actors);

        List<Person> directors = Arrays.asList(
            TestData.STEVEN_SPIELBERG,
            TestData.CHRISTOPHER_NOLAN
        );
        personRepository.saveAll(directors);

        Movie movie1 = TestData.createSampleMovie1();
        Movie movie2 = TestData.createSampleMovie2();
        movieRepository.saveAll(Arrays.asList(movie1, movie2));

        MovieDocument movieDoc1 = TestData.createSampleMovieDocument1();
        MovieDocument movieDoc2 = TestData.createSampleMovieDocument2();
        movieSearchRepository.saveAll(Arrays.asList(movieDoc1, movieDoc2));

        MovieVectorDocument movieVectorDoc1 = TestData.createSampleMovieVectorDocument1();
        MovieVectorDocument movieVectorDoc2 = TestData.createSampleMovieVectorDocument2();
        elasticsearchOperations.save(movieVectorDoc1);
        elasticsearchOperations.save(movieVectorDoc2);
    }

    @Transactional
    public void cleanTestData() {
        movieRepository.deleteAll();
        personRepository.deleteAll();
        genreRepository.deleteAll();

        movieSearchRepository.deleteAll();
        elasticsearchOperations.indexOps(MovieVectorDocument.class).delete();
    }
}
