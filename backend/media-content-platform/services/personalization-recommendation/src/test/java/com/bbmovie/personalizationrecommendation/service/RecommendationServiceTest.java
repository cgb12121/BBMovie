package com.bbmovie.personalizationrecommendation.service;

import com.bbmovie.personalizationrecommendation.config.PersonalizationProperties;
import com.bbmovie.personalizationrecommendation.dto.RecommendationResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecommendationServiceTest {

    @Test
    void recommendFiltersSeenMoviesAndRanks() {
        UUID userId = UUID.randomUUID();
        UUID seenMovieId = UUID.randomUUID();
        UUID candidateA = UUID.randomUUID();
        UUID candidateB = UUID.randomUUID();

        CandidateProvider provider = (uid, vec, lim) -> List.of(
                new MovieCandidate(seenMovieId, 0.99, List.of("action"), 0.9, 0.8),
                new MovieCandidate(candidateA, 0.70, List.of("action"), 0.5, 0.4),
                new MovieCandidate(candidateB, 0.80, List.of("drama"), 0.2, 0.2)
        );
        UserFeatureRepository userFeatureRepository = new UserFeatureRepository() {
            @Override
            public Map<String, Double> readGenreAffinity(UUID userId) {
                return Map.of("action", 1.0, "drama", 0.1);
            }

            @Override
            public Set<UUID> readSeenMovieIds(UUID userId) {
                return Set.of(seenMovieId);
            }
        };
        UserVectorRepository userVectorRepository = new UserVectorRepository(null, new PersonalizationProperties()) {
            @Override
            public List<Double> readUserProfileVector(UUID userId) {
                return List.of(0.1, 0.2, 0.3);
            }
        };

        RecommendationService service = new RecommendationService(
                provider,
                userFeatureRepository,
                userVectorRepository,
                new PersonalizationProperties()
        );

        RecommendationResponse response = service.recommend(userId, 2);

        assertEquals(2, response.items().size());
        assertEquals(candidateA, response.items().get(0).movieId());
        assertEquals(candidateB, response.items().get(1).movieId());
    }
}

