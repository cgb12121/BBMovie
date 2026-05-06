package bbmovie.transcode.contracts.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Policy output that guides ladder shaping and encode command tuning.
 *
 * @param recommendedPreset encoder preset recommended by complexity policy
 * @param complexityRiskClass risk bucket derived from complexity analysis (LOW/MEDIUM/HIGH/EXTREME)
 * @param maxRungs max number of renditions allowed after policy filtering
 * @param minBitrateKbps minimum bitrate guardrail in kbps for generated encode commands
 * @param maxBitrateKbps maximum bitrate guardrail in kbps for generated encode commands
 * @param conservativeMode true when policy asks for safer/slower resource behavior
 * @param skipRungs rendition labels to exclude from ladder generation/encoding
 * @param tuningFactors numeric factors used to explain/tune downstream encode behavior
 * @param reasoning human-readable policy rationale entries for logging/debug
 * @param analysisVersion version identifier of feature extraction/analysis logic
 * @param policyVersion version identifier of policy rule set
 * @param encodeBitrateStrategy bitrate control mode expected by encoder layer
 * @param recommendedCrf optional CRF suggestion when strategy supports CRF tuning
 */
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
