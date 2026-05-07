package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Source metadata envelope returned by analysis activities.
 *
 * @param width source width in pixels
 * @param height source height in pixels
 * @param durationSeconds source duration in seconds
 * @param codec source codec name
 * @param sourceProfile optional normalized v2 source profile
 * @param complexityProfile optional v2 complexity profile
 * @param decisionHints optional policy hints derived from complexity profile
 * @param visDecisionReport optional VIS estimate/explainability report payload
 */
public record MetadataDTO(
        int width,
        int height,
        double durationSeconds,
        String codec,
        SourceProfileV2 sourceProfile,
        ComplexityProfileV2 complexityProfile,
        DecisionHintsV2 decisionHints,
        VisDecisionReportDTO visDecisionReport
) implements Serializable {

    public MetadataDTO(int width, int height, double durationSeconds, String codec) {
        this(width, height, durationSeconds, codec, null, null, null, null);
    }

    public MetadataDTO(
            int width,
            int height,
            double durationSeconds,
            String codec,
            SourceProfileV2 sourceProfile,
            ComplexityProfileV2 complexityProfile,
            DecisionHintsV2 decisionHints
    ) {
        this(width, height, durationSeconds, codec, sourceProfile, complexityProfile, decisionHints, null);
    }
}
