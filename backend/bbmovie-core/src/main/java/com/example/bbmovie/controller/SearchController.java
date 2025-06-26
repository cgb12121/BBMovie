package com.example.bbmovie.controller;

import ai.djl.translate.TranslateException;
import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.service.elasticsearch.EmbeddingSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final EmbeddingSearchService embeddingSearchService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> localEmbeddingGetAllMovies() throws IOException {
        List<?> movies = embeddingSearchService.getAllMovies();
        return ResponseEntity.ok(ApiResponse.success(movies));
    }

    @GetMapping("/similar-search")
    public ResponseEntity<ApiResponse<List<?>>> localEmbeddingSemanticSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit
    ) throws TranslateException, IOException {
        List<?> results = embeddingSearchService.searchSimilarMovies(query, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }
}