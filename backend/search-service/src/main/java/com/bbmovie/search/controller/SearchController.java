package com.bbmovie.search.controller;

import com.bbmovie.search.dto.ApiResponse;
import com.bbmovie.search.dto.PageResponse;
import com.bbmovie.search.dto.SearchCriteria;
import com.bbmovie.search.service.search.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
//        return exchange.getPrincipal()
//                .cast(JwtAuthenticationToken.class)
//                .flatMap(auth -> {
//                    Jwt jwt = auth.getToken();
//                    String region = jwt.getClaimAsString("region");
//                    int age = jwt.getClaim("age");
//
//                    return searchService.getAllMovies(page, size, age, region, Object.class)
//                            .map(pageResult -> ResponseEntity.ok(ApiResponse.success(pageResult)));
//                });
        return searchService.getAllMovies(page, size, 100, "", Object.class)
                .map(pageResult -> ResponseEntity.ok(ApiResponse.success(pageResult)));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<PageResponse<Object>>>> searchMovies(
            @ModelAttribute SearchCriteria criteria,
            ServerWebExchange exchange) {
        criteria.setPage(criteria.getPage() != null ? criteria.getPage() : 0);
        criteria.setSize(criteria.getSize() != null ? criteria.getSize() : 20);

        // criteria automatically populated from query params
//        return exchange.getPrincipal()
//                .cast(JwtAuthenticationToken.class)
//                .flatMap(auth -> {
//                    Jwt jwt = auth.getToken();
//                    criteria.setRegion(jwt.getClaimAsString("region"));
//                    criteria.setAge(jwt.getClaim("age"));
//                    return searchService.searchSimilar(criteria, Object.class)
//                            .map(pageResult -> ResponseEntity.ok(ApiResponse.success(pageResult)));
//                });
        return searchService.searchSimilar(criteria, Object.class)
                .map(pageResult -> ResponseEntity.ok(ApiResponse.success(pageResult)));
    }
}
