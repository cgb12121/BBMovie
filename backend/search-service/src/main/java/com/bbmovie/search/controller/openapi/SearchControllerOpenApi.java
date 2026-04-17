package com.bbmovie.search.controller.openapi;

import com.bbmovie.search.dto.ApiResponse;
import com.bbmovie.search.dto.PageResponse;
import com.bbmovie.search.dto.SearchCriteria;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@SuppressWarnings("unused")
@Tag(name = "Search", description = "Movie search and discovery APIs")
public interface SearchControllerOpenApi {
    @Operation(summary = "List all movies", description = "Returns paginated movie list")
    Mono<ResponseEntity<ApiResponse<PageResponse<Object>>>> getAllMovies(
            ServerWebExchange exchange,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @Operation(summary = "Search movies", description = "Search movies using query criteria")
    Mono<ResponseEntity<ApiResponse<PageResponse<Object>>>> searchMovies(
            @ModelAttribute SearchCriteria criteria,
            ServerWebExchange exchange
    );
}

