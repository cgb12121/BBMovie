package bbmovie.transcode.vis.probe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import bbmovie.transcode.vis.dto.VisProbeOutcome;
import bbmovie.transcode.vis.dto.VisSourceVideoMetadata;

import java.util.List;

/**
 * Preferred VIS probe strategy using short-lived presigned URL + ffprobe.
 *
 * <p>Highest priority strategy because it avoids local object copy while preserving full probe fidelity.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisPresignedUrlProbeStrategy implements VisProbeStrategy {

    private final VisPresignedUrlService presignedUrlService;
    private final VisMetadataService metadataService;
    private final VisLadderGenerationService ladderGenerationService;

    /** Strategy display name used in logs/diagnostics. */
    @Override
    public String getName() {
        return "PresignedUrl";
    }

    /** Supports common video container extensions expected by VIS pipeline. */
    @Override
    public boolean supports(String bucket, String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.endsWith(".mp4")
                || lowerKey.endsWith(".mkv")
                || lowerKey.endsWith(".mov")
                || lowerKey.endsWith(".webm")
                || lowerKey.endsWith(".avi");
    }

    @Override
    public int getPriority() {
        return 100;
    }

    /** Probes via presigned URL and derives ladder/cost outputs. */
    @Override
    public VisProbeOutcome probe(String bucket, String key) {
        try {
            String url = presignedUrlService.generateProbeUrl(bucket, key);
            VisSourceVideoMetadata metadata = metadataService.getMetadataFromUrl(url);
            List<VisLadderGenerationService.LadderRung> resolutions = ladderGenerationService.generateEncodingLadder(metadata);
            List<String> suffixes = ladderGenerationService.toSuffixes(resolutions);
            int peakCost = ladderGenerationService.calculatePeakCost(suffixes);
            int totalCost = ladderGenerationService.calculateTotalCost(suffixes);
            log.info("VIS probed {}/{} via presigned URL: {}x{}, rungs={}", bucket, key, metadata.width(), metadata.height(), suffixes);
            return new VisProbeOutcome(
                    metadata.width(),
                    metadata.height(),
                    metadata.duration(),
                    metadata.codec(),
                    suffixes,
                    peakCost,
                    totalCost
            );
        } catch (Exception e) {
            throw new VisProbeStrategy.VisProbeException("Presigned URL probe failed: " + e.getMessage(), e);
        }
    }
}

