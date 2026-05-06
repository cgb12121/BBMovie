package bbmovie.transcode.cas.analysis;

import java.time.Instant;
import java.util.Map;

/**
 * CAS output profile (ported from transcode-worker {@code ComplexityProfile}).
 */
public record ComplexityProfile(
        String uploadId,
        String contentClass,
        double complexityScore,
        Map<String, Double> featureScores,
        RecipeHints recipeHints,
        Instant analyzedAt
) {
    /** Returns a neutral profile used when complexity analysis is disabled/unavailable. */
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
