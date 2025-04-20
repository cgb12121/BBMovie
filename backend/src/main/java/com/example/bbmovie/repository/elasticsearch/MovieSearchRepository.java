//package com.example.bbmovie.repository.elasticsearch;
//
//import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
//import com.example.bbmovie.model.elasticsearch.MovieDocument;
//
//import java.util.List;
//
//public interface MovieSearchRepository extends ElasticsearchRepository<MovieDocument, String> {
//    List<MovieDocument> findByTitleContainingOrDescriptionContaining(String title, String description);
//}