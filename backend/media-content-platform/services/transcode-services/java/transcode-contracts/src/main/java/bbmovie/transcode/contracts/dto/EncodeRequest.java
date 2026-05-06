package bbmovie.transcode.contracts.dto;

import java.io.Serializable;

/**
 * Input payload for one encode activity execution.
 *
 * @param uploadId logical upload identifier shared by all renditions of the same source
 * @param resolution rendition label (e.g. {@code 1080p}, {@code 720p}) used in output pathing
 * @param width target encoded width in pixels
 * @param height target encoded height in pixels
 * @param masterKeyHex optional encryption key material (hex) for HLS packaging flows
 * @param masterIvHex optional encryption IV material (hex) for HLS packaging flows
 * @param sourceBucket source object bucket that stores original uploaded media
 * @param sourceKey source object key within {@code sourceBucket}
 * @param preferredPreset optional encoder preset override (e.g. {@code veryfast}, {@code medium})
 * @param minBitrateKbps optional lower bitrate guardrail in kbps
 * @param maxBitrateKbps optional upper bitrate guardrail in kbps
 * @param conservativeMode whether policy requested conservative encoding behavior
 * @param bitrateStrategy selected bitrate control strategy for encoder command construction
 * @param encoderCrf optional CRF value when strategy uses CRF-cap style control
 */
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
        boolean conservativeMode,
        EncodeBitrateStrategy bitrateStrategy,
        Integer encoderCrf
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
        this(uploadId, resolution, width, height, masterKeyHex, masterIvHex, sourceBucket, sourceKey, null, null, null, false, null, null);
    }
}
