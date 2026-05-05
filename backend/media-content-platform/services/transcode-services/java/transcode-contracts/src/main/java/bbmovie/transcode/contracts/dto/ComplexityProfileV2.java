package bbmovie.transcode.contracts.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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
