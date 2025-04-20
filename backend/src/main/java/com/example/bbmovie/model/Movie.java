package com.example.bbmovie.model;

import com.example.bbmovie.model.base.BaseEntity;
import com.example.bbmovie.model.enumerate.ContentRating;
import com.example.bbmovie.model.enumerate.VideoQuality;
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

    @Column(name = "movie_url")
    private String movieUrl;

    @Column(name = "rating")
    private Double rating;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_genres",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @ToString.Exclude
    private Set<Genre> genres = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_actors",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "actor_id")
    )
    @ToString.Exclude
    private Set<Actor> actors = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "movie_directors",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "director_id")
    )
    @ToString.Exclude
    private Set<Director> directors = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private Set<Review> reviews = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "content_rating")
    private ContentRating contentRating;

    @ElementCollection(targetClass = VideoQuality.class)
    @CollectionTable(name = "movie_video_qualities", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "video_quality")
    @Enumerated(EnumType.STRING)
    private Set<VideoQuality> videoQuality = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "movie_production_companies", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "company")
    private Set<String> productionCompanies = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "movie_production_countries", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "country")
    private Set<String> productionCountries = new HashSet<>();

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @ElementCollection
    @CollectionTable(name = "movie_tags", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "movie_categories", joinColumns = @JoinColumn(name = "movie_id"))
    @Column(name = "category")
    private Set<String> categories = new HashSet<>();

    @Column(name = "is_active")
    private Boolean isActive = true;
}
