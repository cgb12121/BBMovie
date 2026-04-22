package com.bbmovie.transcodeworker.service.complexity.dto;

import java.time.Instant;
import java.util.Map;

/**
 * CAS output profile used for ladder/recipe tuning.
 */
public record ComplexityProfile(
        String uploadId,
        String contentClass,
        double complexityScore,
        Map<String, Double> featureScores,
        RecipeHints recipeHints,
        Instant analyzedAt
) {
    public static ComplexityProfile basic(String uploadId) {
        return new ComplexityProfile(
                uploadId,
                "unknown",
                0.0,
                Map.of(),
                RecipeHints.none(),
                Instant.now()
        );
    }
}
