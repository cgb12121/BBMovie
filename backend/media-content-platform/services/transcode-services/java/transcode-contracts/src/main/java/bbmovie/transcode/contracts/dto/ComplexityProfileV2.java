package bbmovie.transcode.contracts.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Structured complexity analysis result consumed by orchestration and encode policy.
 *
 * @param uploadId logical upload identifier this profile belongs to
 * @param complexityScore normalized aggregate complexity score in range [0,1]
 * @param riskClass coarse complexity class derived from {@code complexityScore}
 * @param dimensionScores per-dimension normalized scores used to compute final complexity
 * @param decisionHints policy outputs derived from this profile (ladder + encode tuning)
 * @param topFactors short ranked explanation strings for dominant dimensions
 * @param analysisVersion version identifier for analysis algorithm
 * @param policyVersion version identifier for decision-policy mapping
 * @param confidence confidence of source/profile quality used for this analysis
 * @param fallbackReason non-empty when profile was produced by fallback path
 * @param analyzedAt analysis timestamp
 */
public record ComplexityProfileV2(
        String uploadId,
        double complexityScore,
        String riskClass,
        Map<String, Double> dimensionScores,
        DecisionHintsV2 decisionHints,
        List<String> topFactors,
        String analysisVersion,
        String policyVersion,
        double confidence,
        String fallbackReason,
        Instant analyzedAt
) implements Serializable {
}
