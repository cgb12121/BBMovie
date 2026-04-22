package com.bbmovie.personalizationrecommendation.service;

import com.bbmovie.personalizationrecommendation.config.PersonalizationProperties;
import com.bbmovie.personalizationrecommendation.dto.RecommendationItem;
import com.bbmovie.personalizationrecommendation.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final CandidateProvider candidateProvider;
    private final UserFeatureRepository userFeatureRepository;
    private final UserVectorRepository userVectorRepository;
    private final PersonalizationProperties properties;

    public RecommendationResponse recommend(UUID userId, int limit) {
        int effectiveLimit = Math.max(1, Math.min(limit, 100));
        Map<String, Double> genreAffinity = userFeatureRepository.readGenreAffinity(userId);
        Set<UUID> seenMovies = userFeatureRepository.readSeenMovieIds(userId);
        List<Double> profileVector = userVectorRepository.readUserProfileVector(userId);
        List<MovieCandidate> candidates = candidateProvider.candidates(userId, profileVector, effectiveLimit * 3);

        List<RecommendationItem> items = new ArrayList<>();
        for (MovieCandidate candidate : candidates) {
            if (seenMovies.contains(candidate.movieId())) {
                continue;
            }
            double score = score(candidate, genreAffinity);
            String reason = reason(candidate, genreAffinity);
            items.add(new RecommendationItem(candidate.movieId(), score, reason));
        }
        items.sort(Comparator.comparingDouble(RecommendationItem::score).reversed());
        if (items.size() > effectiveLimit) {
            items = new ArrayList<>(items.subList(0, effectiveLimit));
        }
        return new RecommendationResponse(userId, effectiveLimit, items);
    }

    private double score(MovieCandidate candidate, Map<String, Double> genreAffinity) {
        double vector = candidate.vectorScore() * properties.getRanking().getVectorWeight();
        double genre = matchingGenreAffinity(candidate.genres(), genreAffinity) * properties.getRanking().getGenreWeight();
        double popularity = candidate.popularity() * properties.getRanking().getPopularityWeight();
        double freshness = candidate.freshness() * properties.getRanking().getFreshnessWeight();
        return vector + genre + popularity + freshness;
    }

    private String reason(MovieCandidate candidate, Map<String, Double> genreAffinity) {
        double genreScore = matchingGenreAffinity(candidate.genres(), genreAffinity);
        if (genreScore > 0.4) {
            return "Matches your favorite genres";
        }
        if (candidate.popularity() > 0.6) {
            return "Popular with similar viewers";
        }
        return "Similar to your recent watches";
    }

    private double matchingGenreAffinity(List<String> genres, Map<String, Double> affinity) {
        if (genres == null || genres.isEmpty() || affinity.isEmpty()) {
            return 0.0;
        }
        double max = 0.0;
        for (String genre : genres) {
            if (genre == null) {
                continue;
            }
            max = Math.max(max, affinity.getOrDefault(genre, 0.0));
        }
        return max;
    }
}

