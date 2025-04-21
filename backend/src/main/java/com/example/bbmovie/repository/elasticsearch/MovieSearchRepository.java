package com.example.bbmovie.repository.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import com.example.bbmovie.model.elasticsearch.MovieDocument;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieSearchRepository extends ElasticsearchRepository<MovieDocument, String> {
    List<MovieDocument> findByTitleContainingOrDescriptionContaining(String title, String description);
}