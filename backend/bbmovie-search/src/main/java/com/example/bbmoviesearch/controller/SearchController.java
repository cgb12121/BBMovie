package com.example.bbmoviesearch.controller;

import com.example.bbmoviesearch.service.elasticsearch.EmbeddingSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final EmbeddingSearchService embeddingSearchService;

    @Autowired
    public SearchController(EmbeddingSearchService embeddingSearchService) {
        this.embeddingSearchService = embeddingSearchService;
    }

    @GetMapping("/all")
    public Mono<ResponseEntity<List<Object>>> getAllMovies() {
        return embeddingSearchService.getAllMovies()
                .map(movies -> ResponseEntity.ok().body(movies));
    }

    @GetMapping("/similar-search")
    public Mono<ResponseEntity<List<Object>>> searchSimilarMovies(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        return embeddingSearchService.searchSimilarMovies(query, limit)
                .map(results -> ResponseEntity.ok().body(results));
    }
}
