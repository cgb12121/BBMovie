package com.bbmovie.personalizationrecommendation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "personalization")
public class PersonalizationProperties {

    private final Qdrant qdrant = new Qdrant();
    private final Ranking ranking = new Ranking();
    private final Redis redis = new Redis();

    @Getter
    @Setter
    public static class Qdrant {
        private boolean enabled = true;
        private String baseUrl = "http://localhost:6333";
        private String collection = "movie_vectors";
        private int candidateLimit = 200;
        private String vectorKeyPrefix = "user:profile:vector:";
    }

    @Getter
    @Setter
    public static class Ranking {
        private double vectorWeight = 0.60;
        private double genreWeight = 0.20;
        private double popularityWeight = 0.15;
        private double freshnessWeight = 0.05;
    }

    @Getter
    @Setter
    public static class Redis {
        private String userGenreAffinityPrefix = "user:affinity:genre:";
        private String userSeenMoviesPrefix = "user:seen:movies:";
    }
}

