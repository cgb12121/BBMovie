package com.example.bbmovie.service.elasticsearch.sample;

import com.example.bbmovie.model.*;
import com.example.bbmovie.model.enumerate.ContentRating;
import com.example.bbmovie.model.enumerate.VideoQuality;
import com.example.bbmovie.model.elasticsearch.MovieDocument;
import com.example.bbmovie.model.elasticsearch.MovieVectorDocument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;

public class SampleData {
    
    public static final Genre ACTION = createGenre("Action");
    public static final Genre DRAMA = createGenre("Drama");
    public static final Genre COMEDY = createGenre("Comedy");
    public static final Genre SCI_FI = createGenre("Science Fiction");
    
    public static final Actor TOM_HANKS = createActor("Tom Hanks");
    public static final Actor LEONARDO_DICAPRIO = createActor("Leonardo DiCaprio");
    public static final Actor MERYL_STREEP = createActor("Meryl Streep");
    
    public static final Director STEVEN_SPIELBERG = createDirector("Steven Spielberg");
    public static final Director CHRISTOPHER_NOLAN = createDirector("Christopher Nolan");
    
    public static Movie createSampleMovie1() {
        Movie movie = new Movie();
        movie.setTitle("The Matrix");
        movie.setDescription("A computer hacker learns about the true nature of reality and his role in the war against its controllers.");
        movie.setReleaseDate(LocalDate.of(1999, 3, 31));
        movie.setDurationMinutes(136);
        movie.setPosterUrl("https://example.com/matrix-poster.jpg");
        movie.setBackdropUrl("https://example.com/matrix-backdrop.jpg");
        movie.setTrailerUrl("https://example.com/matrix-trailer.mp4");
        movie.setMovieUrl("https://example.com/matrix-movie.mp4");
        movie.setRating(8.7);
        movie.setContentRating(ContentRating.R);
        movie.setVideoQuality(new HashSet<>(Arrays.asList(VideoQuality.VIDEO_QUALITY_720P, VideoQuality.VIDEO_QUALITY_1080P)));
        movie.setProductionCompanies(new HashSet<>(Arrays.asList("Warner Bros.", "Village Roadshow Pictures")));
        movie.setProductionCountries(new HashSet<>(Arrays.asList("USA", "Australia")));
        movie.setViewCount(1000000L);
        movie.setTags(new HashSet<>(Arrays.asList("cyberpunk", "action", "sci-fi")));
        movie.setCategories(new HashSet<>(Arrays.asList("Action", "Sci-Fi")));
        movie.setIsActive(true);
        
        movie.setGenres(new HashSet<>(Arrays.asList(ACTION, SCI_FI)));
        movie.setActors(new HashSet<>(Arrays.asList(TOM_HANKS, LEONARDO_DICAPRIO)));
        movie.setDirectors(new HashSet<>(Arrays.asList(CHRISTOPHER_NOLAN)));
        
        return movie;
    }
    
    public static Movie createSampleMovie2() {
        Movie movie = new Movie();
        movie.setTitle("Forrest Gump");
        movie.setDescription("The presidencies of Kennedy and Johnson, the Vietnam War, the Watergate scandal and other historical events unfold from the perspective of an Alabama man with an IQ of 75.");
        movie.setReleaseDate(LocalDate.of(1994, 7, 6));
        movie.setDurationMinutes(142);
        movie.setPosterUrl("https://example.com/forrest-gump-poster.jpg");
        movie.setBackdropUrl("https://example.com/forrest-gump-backdrop.jpg");
        movie.setTrailerUrl("https://example.com/forrest-gump-trailer.mp4");
        movie.setMovieUrl("https://example.com/forrest-gump-movie.mp4");
        movie.setRating(8.8);
        movie.setContentRating(ContentRating.PG_13);
        movie.setVideoQuality(new HashSet<>(Arrays.asList(VideoQuality.VIDEO_QUALITY_720P)));
        movie.setProductionCompanies(new HashSet<>(Arrays.asList("Paramount Pictures")));
        movie.setProductionCountries(new HashSet<>(Arrays.asList("USA")));
        movie.setViewCount(2000000L);
        movie.setTags(new HashSet<>(Arrays.asList("drama", "romance", "historical")));
        movie.setCategories(new HashSet<>(Arrays.asList("Drama", "Romance")));
        movie.setIsActive(true);
        
        movie.setGenres(new HashSet<>(Arrays.asList(DRAMA, COMEDY)));
        movie.setActors(new HashSet<>(Arrays.asList(TOM_HANKS, MERYL_STREEP)));
        movie.setDirectors(new HashSet<>(Arrays.asList(STEVEN_SPIELBERG)));
        
        return movie;
    }
    
    public static MovieDocument createSampleMovieDocument1() {
        return MovieDocument.builder()
                .id("1")
                .title("The Matrix")
                .description("A computer hacker learns about the true nature of reality and his role in the war against its controllers.")
                .rating(8.7)
                .categories(Arrays.asList("Action", "Sci-Fi"))
                .posterUrl("https://example.com/matrix-poster.jpg")
                .releaseDate(LocalDateTime.of(1999, 3, 31, 0, 0))
                .build();
    }
    
    public static MovieDocument createSampleMovieDocument2() {
        return MovieDocument.builder()
                .id("2")
                .title("Forrest Gump")
                .description("The presidencies of Kennedy and Johnson, the Vietnam War, the Watergate scandal and other historical events unfold from the perspective of an Alabama man with an IQ of 75.")
                .rating(8.8)
                .categories(Arrays.asList("Drama", "Romance"))
                .posterUrl("https://example.com/forrest-gump-poster.jpg")
                .releaseDate(LocalDateTime.of(1994, 7, 6, 0, 0))
                .build();
    }
    
    public static MovieVectorDocument createSampleMovieVectorDocument1() {
        return MovieVectorDocument.builder()
                .id("1")
                .title("The Matrix")
                .description("A computer hacker learns about the true nature of reality and his role in the war against its controllers.")
                .contentVector(new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f})
                .build();
    }
    
    public static MovieVectorDocument createSampleMovieVectorDocument2() {
        return MovieVectorDocument.builder()
                .id("2")
                .title("Forrest Gump")
                .description("The presidencies of Kennedy and Johnson, the Vietnam War, the Watergate scandal and other historical events unfold from the perspective of an Alabama man with an IQ of 75.")
                .contentVector(new float[]{0.6f, 0.7f, 0.8f, 0.9f, 1.0f})
                .build();
    }
    
    private static Genre createGenre(String name) {
        Genre genre = new Genre();
        genre.setName(name);
        return genre;
    }
    
    private static Actor createActor(String name) {
        Actor actor = new Actor();
        actor.setName(name);
        return actor;
    }
    
    private static Director createDirector(String name) {
        Director director = new Director();
        director.setName(name);
        return director;
    }
} 