//package com.example.bbmovie.controller;
//
//import com.example.bbmovie.dto.request.MovieCreateRequest;
//import com.example.bbmovie.model.Movie;
//import com.example.bbmovie.model.elasticsearch.MovieDocument;
//import com.example.bbmovie.model.elasticsearch.MovieVectorDocument;
//import com.example.bbmovie.service.elasticsearch.MovieSearchService;
//import com.example.bbmovie.service.elasticsearch.MovieVectorSearchService;
//import com.example.bbmovie.service.intf.CloudinaryService;
//import com.example.bbmovie.service.intf.MovieService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.web.bind.annotation.*;
//
//import java.io.IOException;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/movies")
//@RequiredArgsConstructor
//public class MovieController {
//
//    private final MovieSearchService movieSearchService;
//    private final MovieService movieService;
//    private final MovieVectorSearchService movieVectorSearchService;
//    private final CloudinaryService cloudinaryService;
//
//    @GetMapping("/search")
//    public ResponseEntity<List<MovieDocument>> searchMovies(
//            @RequestParam String query,
//            @RequestParam(required = false) List<String> categories,
//            @RequestParam(required = false) Double minRating) throws IOException {
//        return ResponseEntity.ok(movieSearchService.searchMovies(query));
//    }
//
//    @PostMapping("/reindex")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> reindexMovies() {
//        List<Movie> allMovies = movieService.getAllMovies();
//        movieSearchService.reindexAll(allMovies);
//        return ResponseEntity.ok().build();
//    }
//
//    @GetMapping("/semantic-search")
//    public ResponseEntity<List<MovieVectorDocument>> semanticSearch(
//            @RequestParam String query,
//            @RequestParam(defaultValue = "10") int limit) {
//        try {
//            List<MovieVectorDocument> results = movieVectorSearchService.searchSimilarMovies(query, limit);
//            return ResponseEntity.ok(results);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    @PostMapping("/index")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<Void> indexMovie(@RequestBody Movie movie) {
//        try {
//            movieVectorSearchService.indexMovieWithVector(movie);
//            return ResponseEntity.ok().build();
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    @PostMapping(value = "/upload",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @PreAuthorize("hasRole('ADMIN') or hasRole('MODERATOR')")
//    public ResponseEntity<Movie> createMovie(@ModelAttribute MovieCreateRequest request) throws IOException {
//        String posterUrl = cloudinaryService.uploadImage(request.getPoster());
//        Movie movie = movieService.createMovie(request, posterUrl);
//        return ResponseEntity.ok(movie);
//    }
//}