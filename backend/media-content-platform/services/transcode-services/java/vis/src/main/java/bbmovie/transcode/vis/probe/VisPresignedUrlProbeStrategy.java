package bbmovie.transcode.vis.probe;

import bbmovie.transcode.lgs.analysis.LgsLadderGenerationService;
import bbmovie.transcode.lgs.analysis.LgsSourceVideoMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ported from transcode-worker {@code PresignedUrlProbeStrategy}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VisPresignedUrlProbeStrategy implements VisProbeStrategy {

    private final VisPresignedUrlService presignedUrlService;
    private final VisMetadataService metadataService;
    private final LgsLadderGenerationService ladderGenerationService;

    @Override
    public String getName() {
        return "PresignedUrl";
    }

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

    @Override
    public VisProbeOutcome probe(String bucket, String key) {
        try {
            String url = presignedUrlService.generateProbeUrl(bucket, key);
            LgsSourceVideoMetadata metadata = metadataService.getMetadataFromUrl(url);
            List<LgsLadderGenerationService.LadderRung> resolutions = ladderGenerationService.generateEncodingLadder(metadata);
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

