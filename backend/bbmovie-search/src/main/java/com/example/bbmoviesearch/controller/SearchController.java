package com.example.bbmoviesearch.controller;

import com.example.bbmoviesearch.dto.ApiResponse;
import com.example.bbmoviesearch.dto.PageResponse;
import com.example.bbmoviesearch.service.elasticsearch.SearchCriteria;
import com.example.bbmoviesearch.service.elasticsearch.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/search")
public class SearchController {

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
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

                    return searchService.getAllMovies(page, size, age, region, Object.class)
                            .map(pageResult -> ResponseEntity.ok(ApiResponse.success(pageResult)));
                });
    }

    @GetMapping("/search")
    public Mono<ResponseEntity<ApiResponse<PageResponse<Object>>>> searchMovies(
            @ModelAttribute SearchCriteria criteria,
            ServerWebExchange exchange) {
        // criteria automatically populated from query params
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    Jwt jwt = auth.getToken();
                    criteria.setRegion(jwt.getClaimAsString("region"));
                    criteria.setAge(jwt.getClaim("age"));
                    return searchService.searchSimilarMovies(criteria, Object.class)
                            .map(pageResult -> ResponseEntity.ok(ApiResponse.success(pageResult)));
                });
    }
}
