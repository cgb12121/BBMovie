package bbmovie.transcode.cas.analysis;

import bbmovie.transcode.contracts.dto.DecisionHintsV2;
import bbmovie.transcode.contracts.dto.EncodeBitrateStrategy;
import bbmovie.transcode.contracts.dto.SourceProfileV2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DecisionHintsPolicyEngine {

    private final String analysisVersion;
    private final String policyVersion;

    public DecisionHintsPolicyEngine(
            @Value("${cas.profile-v2.analysis-version:v2.0}") String analysisVersion,
            @Value("${cas.profile-v2.policy-version:policy-v1}") String policyVersion) {
        this.analysisVersion = analysisVersion;
        this.policyVersion = policyVersion;
    }

    public DecisionHintsV2 buildHints(SourceProfileV2 source, ComplexityRiskClass riskClass, Map<String, Double> dimensions) {
        boolean conservativeMode = riskClass == ComplexityRiskClass.HIGH || riskClass == ComplexityRiskClass.EXTREME;
        String preset = switch (riskClass) {
            case EXTREME -> "slow";
            case HIGH -> "medium";
            case MEDIUM -> "veryfast";
            case LOW -> "superfast";
        };
        int maxRungs = switch (riskClass) {
            case EXTREME, HIGH -> 4;
            case MEDIUM -> 5;
            case LOW -> 6;
        };
        List<String> skip = new ArrayList<>();
        if (source.height() < 1080) {
            skip.add("1080p");
        }
        if (source.height() < 720) {
            skip.add("720p");
        }
        if (source.height() < 480) {
            skip.add("480p");
        }
        if (conservativeMode && source.durationSeconds() > 90 * 60) {
            skip.add("144p");
        }
        int minBitrate = conservativeMode ? 1100 : 700;
        int maxBitrate = conservativeMode ? 8500 : 6000;
        Map<String, Double> tuningFactors = new HashMap<>();
        tuningFactors.put("motionScore", dimensions.getOrDefault("motionScore", 0.0));
        tuningFactors.put("noiseScore", dimensions.getOrDefault("noiseScore", 0.0));
        tuningFactors.put("spatialScore", dimensions.getOrDefault("spatialScore", 0.0));
        List<String> reasoning = new ArrayList<>();
        reasoning.add("riskClass=" + riskClass.name());
        reasoning.add("durationSeconds=" + source.durationSeconds());
        reasoning.add("sourceResolution=" + source.width() + "x" + source.height());
        EncodeBitrateStrategy encodeBitrateStrategy =
                conservativeMode ? EncodeBitrateStrategy.VBV_CRF_CAP : EncodeBitrateStrategy.VBV_ABR;
        Integer recommendedCrf = conservativeMode ? Integer.valueOf(22) : null;
        return new DecisionHintsV2(
                preset,
                riskClass.name(),
                maxRungs,
                minBitrate,
                maxBitrate,
                conservativeMode,
                skip,
                tuningFactors,
                reasoning,
                analysisVersion,
                policyVersion,
                encodeBitrateStrategy,
                recommendedCrf
        );
    }
}
