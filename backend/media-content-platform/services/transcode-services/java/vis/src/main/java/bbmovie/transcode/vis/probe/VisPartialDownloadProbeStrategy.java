package bbmovie.transcode.vis.probe;

import bbmovie.transcode.lgs.analysis.LgsLadderGenerationService;
import bbmovie.transcode.lgs.analysis.LgsSourceVideoMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Ported from transcode-worker {@code PartialDownloadProbeStrategy}.
 */
@Slf4j
@Component
public class VisPartialDownloadProbeStrategy implements VisProbeStrategy {

    private final VisMinioPartialDownloadService downloadService;
    private final VisMetadataService metadataService;
    private final LgsLadderGenerationService ladderGenerationService;
    private final int partialSizeMb;

    public VisPartialDownloadProbeStrategy(
            VisMinioPartialDownloadService downloadService,
            VisMetadataService metadataService,
            LgsLadderGenerationService ladderGenerationService,
            @Value("${app.vis.probe.partial-size-mb:10}") int partialSizeMb) {
        this.downloadService = downloadService;
        this.metadataService = metadataService;
        this.ladderGenerationService = ladderGenerationService;
        this.partialSizeMb = partialSizeMb;
    }

    @Override
    public String getName() {
        return "PartialDownload";
    }

    @Override
    public boolean supports(String bucket, String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.endsWith(".mp4") || lowerKey.endsWith(".mov");
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public VisProbeOutcome probe(String bucket, String key) {
        Path tempFile = null;
        try {
            long partialBytes = partialSizeMb * 1024L * 1024L;
            byte[] partialData = downloadService.downloadPartial(bucket, key, partialBytes);
            tempFile = Files.createTempFile("vis_probe_", ".partial");
            Files.write(tempFile, partialData);
            LgsSourceVideoMetadata metadata = metadataService.getMetadata(tempFile);
            List<LgsLadderGenerationService.LadderRung> resolutions = ladderGenerationService.generateEncodingLadder(metadata);
            List<String> suffixes = ladderGenerationService.toSuffixes(resolutions);
            int peakCost = ladderGenerationService.calculatePeakCost(suffixes);
            int totalCost = ladderGenerationService.calculateTotalCost(suffixes);
            log.info("VIS probed {}/{} (partial): {}x{}, rungs={}", bucket, key, metadata.width(), metadata.height(), suffixes);
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
            throw new VisProbeStrategy.VisProbeException("Partial download probe failed: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
