package com.example.bbmoviesearch.controller;

import ai.djl.translate.TranslateException;
import com.example.bbmoviesearch.service.elasticsearch.EmbeddingSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final EmbeddingSearchService embeddingSearchService;

    @GetMapping("/all")
    public ResponseEntity<?> localEmbeddingGetAllMovies() throws IOException {
        List<?> movies = embeddingSearchService.getAllMovies();
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/similar-search")
    public ResponseEntity<List<?>> localEmbeddingSemanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) throws TranslateException, IOException {
        List<?> results = embeddingSearchService.searchSimilarMovies(query, limit);
        return ResponseEntity.ok(results);
    }
}