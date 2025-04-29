package com.example.bbmovie.controller;

import ai.djl.translate.TranslateException;
import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.service.elasticsearch.huggingface.MovieVectorSearchService;
import com.example.bbmovie.service.elasticsearch.local.LocalEmbeddingSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Log4j2
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final MovieVectorSearchService movieVectorSearchService;
    private final LocalEmbeddingSearchService localEmbeddingSearchService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> localEmbeddingGetAllMovies() throws IOException {
        List<?> movies = localEmbeddingSearchService.getAllMovies();
        return ResponseEntity.ok(ApiResponse.success(movies));
    }

    @GetMapping("/similar-search")
    public ResponseEntity<ApiResponse<List<?>>> localEmbeddingSemanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) throws TranslateException, IOException {
        List<?> results = localEmbeddingSearchService.searchSimilarMovies(query, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/hugging-face-api/all")
    public ResponseEntity<ApiResponse<?>> getAllMovies() throws Exception {
        List<?> movies = movieVectorSearchService.getAllMovies();
        return ResponseEntity.ok(ApiResponse.success(movies));
    }

    @GetMapping("/hugging-face-api/semantic-search")
    public ResponseEntity<ApiResponse<List<?>>> semanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) throws IOException {
        List<?> results = movieVectorSearchService.searchSimilarMovies(query, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}