package com.bbmovie.personalizationrecommendation.service;

import com.bbmovie.personalizationrecommendation.config.PersonalizationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "personalization.qdrant.enabled", havingValue = "true", matchIfMissing = true)
public class QdrantCandidateProvider implements CandidateProvider {

    private final RestClient.Builder restClientBuilder;
    private final PersonalizationProperties properties;

    @Override
    @SuppressWarnings("unchecked")
    public List<MovieCandidate> candidates(UUID userId, List<Double> profileVector, int limit) {
        if (profileVector == null || profileVector.isEmpty()) {
            return List.of();
        }
        int candidateLimit = Math.max(limit, properties.getQdrant().getCandidateLimit());
        String url = properties.getQdrant().getBaseUrl();
        String collection = properties.getQdrant().getCollection();
        Map<String, Object> body = Map.of(
                "vector", profileVector,
                "limit", candidateLimit,
                "with_payload", true
        );
        try {
            Map<String, Object> response = restClientBuilder.baseUrl(url).build()
                    .post()
                    .uri("/collections/{collection}/points/search", collection)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (response == null || !(response.get("result") instanceof List<?> resultList)) {
                return List.of();
            }
            List<MovieCandidate> out = new ArrayList<>();
            for (Object item : resultList) {
                if (!(item instanceof Map<?, ?> row)) {
                    continue;
                }
                Object payloadObj = row.get("payload");
                if (!(payloadObj instanceof Map<?, ?> payload)) {
                    continue;
                }
                UUID movieId = parseMovieId(payload.get("movieId"));
                if (movieId == null) {
                    continue;
                }
                double vectorScore = parseDouble(row.get("score"), 0.0);
                List<String> genres = parseGenres(payload.get("genres"));
                double popularity = parseDouble(payload.get("popularity"), 0.0);
                double freshness = parseDouble(payload.get("freshness"), 0.0);
                out.add(new MovieCandidate(movieId, vectorScore, genres, popularity, freshness));
            }
            return out;
        } catch (Exception ex) {
            log.warn("Qdrant search failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private UUID parseMovieId(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private double parseDouble(Object value, double fallback) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private List<String> parseGenres(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object item : list) {
            if (item != null) {
                out.add(item.toString());
            }
        }
        return out;
    }
}

