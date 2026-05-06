package bbmovie.transcode.contracts.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Normalized source-media profile produced by probe/analysis services.
 *
 * @param uploadId logical upload identifier for correlation across services
 * @param bucket source object bucket name
 * @param key source object key inside {@code bucket}
 * @param width probed source width in pixels
 * @param height probed source height in pixels
 * @param durationSeconds source duration in seconds
 * @param codec primary video codec name
 * @param container container/format name from probe output
 * @param frameRate parsed frame rate as numeric value
 * @param fpsMode inferred frame-rate mode (e.g. {@code cfr}, {@code vfr}, {@code unknown})
 * @param audioChannels detected audio channel count
 * @param bitDepth detected video bit depth when available
 * @param chromaSubsampling detected pixel format/chroma signal
 * @param probeMode label of probe strategy/path that produced this profile
 * @param confidence confidence score for profile completeness/reliability
 * @param analysisVersion version marker for profile extraction logic
 * @param fingerprint short stable fingerprint for dedupe/correlation
 * @param gateReasons optional reasons why probe/analysis selected constrained paths
 */
public record SourceProfileV2(
        String uploadId,
        String bucket,
        String key,
        int width,
        int height,
        double durationSeconds,
        String codec,
        String container,
        double frameRate,
        String fpsMode,
        int audioChannels,
        int bitDepth,
        String chromaSubsampling,
        String probeMode,
        double confidence,
        String analysisVersion,
        String fingerprint,
        List<String> gateReasons
) implements Serializable {
}
