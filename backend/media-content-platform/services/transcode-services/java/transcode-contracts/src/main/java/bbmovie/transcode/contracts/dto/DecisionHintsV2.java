package bbmovie.transcode.contracts.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record DecisionHintsV2(
        String recommendedPreset,
        String complexityRiskClass,
        int maxRungs,
        int minBitrateKbps,
        int maxBitrateKbps,
        boolean conservativeMode,
        List<String> skipRungs,
        Map<String, Double> tuningFactors,
        List<String> reasoning,
        String analysisVersion,
        String policyVersion,
        EncodeBitrateStrategy encodeBitrateStrategy,
        Integer recommendedCrf
) implements Serializable {
}
