package com.example.bbmovie.model;

import com.example.bbmovie.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movies")
@Getter
@Setter
@ToString
public class Movie extends BaseEntity {
    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "backdrop_url")
    private String backdropUrl;

    @Column(name = "trailer_url")
    private String trailerUrl;

    @Column(name = "imdb_rating")
    private Double imdbRating;

    @Column(name = "tmdb_id")
    private String tmdbId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "movie_genres",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "movie_actors",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    private Set<Actor> actors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "movie_directors",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "director_id")
    )
    private Set<Director> directors = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Review> reviews = new HashSet<>();

    @Column(name = "is_active")
    private Boolean isActive = true;
} 