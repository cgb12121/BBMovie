package com.example.bbmoviesearch.controller;

import com.example.bbmoviesearch.dto.ApiResponse;
import com.example.bbmoviesearch.dto.PageResponse;
import com.example.bbmoviesearch.service.elasticsearch.EmbeddingSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final EmbeddingSearchService embeddingSearchService;

    @Autowired
    public SearchController(EmbeddingSearchService embeddingSearchService) {
        this.embeddingSearchService = embeddingSearchService;
    }

    @GetMapping("/all")
    public Mono<ResponseEntity<ApiResponse<PageResponse<Object>>>> getAllMovies(
            ServerWebExchange exchange,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    Jwt jwt = auth.getToken();
                    String region = jwt.getClaimAsString("region");
                    int age = jwt.getClaim("age");

                    return embeddingSearchService.getAllMovies(page, size, age, region, Object.class)
                            .map(pageResult -> ResponseEntity.ok(ApiResponse.success(pageResult)));
                });
    }

    @GetMapping("/similar-search")
    public Mono<ResponseEntity<ApiResponse<PageResponse<Object>>>> searchSimilarMovies(
            ServerWebExchange exchange,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    Jwt jwt = auth.getToken();
                    String region = jwt.getClaimAsString("region");
                    int age = jwt.getClaim("age");

                    return embeddingSearchService.searchSimilarMovies(query, page, size, age, region, Object.class)
                            .map(pageResult -> ResponseEntity.ok(ApiResponse.success(pageResult)));
                });
    }
}
