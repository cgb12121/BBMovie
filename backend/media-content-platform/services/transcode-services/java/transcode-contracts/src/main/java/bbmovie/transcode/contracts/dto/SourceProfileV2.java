package bbmovie.transcode.contracts.dto;

import java.io.Serializable;
import java.util.List;

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
