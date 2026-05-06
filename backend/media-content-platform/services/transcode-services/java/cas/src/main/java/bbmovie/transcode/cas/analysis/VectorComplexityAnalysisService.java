package bbmovie.transcode.cas.analysis;

import bbmovie.transcode.contracts.dto.ComplexityProfileV2;
import bbmovie.transcode.contracts.dto.DecisionHintsV2;
import bbmovie.transcode.contracts.dto.SourceProfileV2;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class VectorComplexityAnalysisService implements ComplexityAnalysisV2Service {

    /**
     * {@link ComplexityProfileV2#fallbackReason()}: empty when this service produced the profile (not conservative fallback).
     */
    private static final String NO_FALLBACK_REASON = "";

    private final DecisionHintsPolicyEngine decisionHintsPolicyEngine;
    private final String analysisVersion;
    private final String policyVersion;

    @Override
    public ComplexityProfileV2 analyze(SourceProfileV2 sourceProfile) {
        Map<String, Double> dimensions = new LinkedHashMap<>();
        double spatialScore = clamp((double) sourceProfile.height() / 2160.0);
        double motionScore = clamp((sourceProfile.frameRate() > 0 ? sourceProfile.frameRate() : 24.0) / 60.0);
        double noiseScore = estimateNoiseScore(sourceProfile.codec(), sourceProfile.bitDepth());
        double sceneChangeDensity = estimateSceneChangeDensity(sourceProfile.durationSeconds(), motionScore);
        dimensions.put("spatialScore", spatialScore);
        dimensions.put("motionScore", motionScore);
        dimensions.put("noiseScore", noiseScore);
        dimensions.put("sceneChangeDensity", sceneChangeDensity);

        double score = clamp(spatialScore * 0.35 + motionScore * 0.3 + noiseScore * 0.2 + sceneChangeDensity * 0.15);
        ComplexityRiskClass riskClass = ComplexityRiskClass.fromScore(score);
        DecisionHintsV2 hints = decisionHintsPolicyEngine.buildHints(sourceProfile, riskClass, dimensions);

        List<String> topFactors = dimensions.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(2)
                .map(e -> e.getKey() + "=" + String.format("%.3f", e.getValue()))
                .toList();
        return new ComplexityProfileV2(
                sourceProfile.uploadId(),
                score,
                riskClass.name(),
                dimensions,
                hints,
                topFactors,
                analysisVersion,
                policyVersion,
                sourceProfile.confidence(),
                NO_FALLBACK_REASON,
                Instant.now()
        );
    }

    private static double estimateNoiseScore(String codec, int bitDepth) {
        double codecFactor = "hevc".equalsIgnoreCase(codec) || "av1".equalsIgnoreCase(codec) ? 0.45 : 0.65;
        double depthBoost = bitDepth >= 10 ? 0.15 : 0.0;
        return clamp(codecFactor + depthBoost);
    }

    private static double estimateSceneChangeDensity(double durationSeconds, double motionScore) {
        if (durationSeconds <= 0) {
            return clamp(0.4 + motionScore * 0.2);
        }
        double normalized = 1.0 / Math.max(1.0, durationSeconds / 600.0);
        return clamp(0.3 * normalized + 0.5 * motionScore);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
