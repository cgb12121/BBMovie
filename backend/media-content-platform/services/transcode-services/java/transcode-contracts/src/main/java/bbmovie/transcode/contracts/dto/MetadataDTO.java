package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record MetadataDTO(
        int width,
        int height,
        double durationSeconds,
        String codec,
        SourceProfileV2 sourceProfile,
        ComplexityProfileV2 complexityProfile,
        DecisionHintsV2 decisionHints
) implements Serializable {

    public MetadataDTO(int width, int height, double durationSeconds, String codec) {
        this(width, height, durationSeconds, codec, null, null, null);
    }
}
