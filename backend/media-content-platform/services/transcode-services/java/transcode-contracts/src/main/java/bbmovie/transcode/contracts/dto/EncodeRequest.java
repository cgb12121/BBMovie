package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

public record EncodeRequest(
        String uploadId,
        String resolution,
        int width,
        int height,
        String masterKeyHex,
        String masterIvHex,
        String sourceBucket,
        String sourceKey,
        String preferredPreset,
        Integer minBitrateKbps,
        Integer maxBitrateKbps,
        boolean conservativeMode
) implements Serializable {

    public EncodeRequest(
            String uploadId,
            String resolution,
            int width,
            int height,
            String masterKeyHex,
            String masterIvHex,
            String sourceBucket,
            String sourceKey) {
        this(uploadId, resolution, width, height, masterKeyHex, masterIvHex, sourceBucket, sourceKey, null, null, null, false);
    }
}
