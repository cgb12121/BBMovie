package com.example.bbmovie.controller;

import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.service.elasticsearch.huggingface.MovieVectorSearchService;
import com.example.bbmovie.service.elasticsearch.local.LocalEmbeddingSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Log4j2
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final MovieVectorSearchService movieVectorSearchService;
    private final LocalEmbeddingSearchService localEmbeddingSearchService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllMovies() {
        try {
            List<?> movies = movieVectorSearchService.getAllMovies();
            return ResponseEntity.ok(movies);
        } catch (Exception e) {
            log.error(e);
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/semantic-search")
    public ResponseEntity<List<?>> semanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            List<?> results = movieVectorSearchService.searchSimilarMovies(query, limit);
            return ResponseEntity.ok(results);
        } catch (IOException e) {
            log.error(e);
            return ResponseEntity.internalServerError().body(Collections.singletonList(ApiResponse.error(e.getMessage())));
        }
    }

    @GetMapping("/local-embedding/all")
    public ResponseEntity<?> localEmbeddingGetAllMovies() {
        try {
            List<?> movies = localEmbeddingSearchService.getAllMovies();
            return ResponseEntity.ok(movies);
        } catch (Exception e) {
            log.error(e);
            return ResponseEntity.status(500).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/local-embedding/semantic-search")
    public ResponseEntity<List<?>> localEmbeddingSemanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            List<?> results = localEmbeddingSearchService.searchSimilarMovies(query, limit);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error(e);
            return ResponseEntity.internalServerError().body(Collections.singletonList(ApiResponse.error(e.getMessage())));
        }
    }
}