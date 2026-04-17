package com.bbmovie.mediaservice.controller.openapi;

import com.bbmovie.mediaservice.controller.UserMovieController.LinkFileRequest;
import com.bbmovie.mediaservice.dto.MovieRequest;
import com.bbmovie.mediaservice.dto.MovieResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "Movies", description = "Movie metadata CRUD APIs")
public interface UserMovieControllerOpenApi {
    @Operation(summary = "Create movie")
    ResponseEntity<MovieResponse> createMovie(@RequestBody MovieRequest request);

    @Operation(summary = "Get movie by ID")
    ResponseEntity<MovieResponse> getMovie(@PathVariable UUID movieId);

    @Operation(summary = "Link media file to movie")
    ResponseEntity<MovieResponse> linkFileToMovie(@PathVariable UUID movieId, @RequestBody LinkFileRequest request);
}

