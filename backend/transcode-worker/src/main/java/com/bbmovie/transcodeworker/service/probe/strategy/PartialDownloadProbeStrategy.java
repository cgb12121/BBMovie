package com.bbmovie.transcodeworker.service.probe.strategy;

import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;
import com.bbmovie.transcodeworker.service.ffmpeg.MetadataService;
import com.bbmovie.transcodeworker.service.ffmpeg.VideoTranscoderService;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeResult;
import com.bbmovie.transcodeworker.service.scheduler.ResolutionCostCalculator;
import com.bbmovie.transcodeworker.service.storage.MinioDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Probing strategy using partial file download.
 * <p>
 * This strategy downloads only the first few MB of the file
 * to extract metadata. Works well for MP4 files with moov
 * atom at the beginning.
 * <p>
 * Fallback strategy when presigned URL fails.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PartialDownloadProbeStrategy implements ProbeStrategy {

    private final MinioDownloadService downloadService;
    private final MetadataService metadataService;
    private final VideoTranscoderService videoTranscoderService;
    private final ResolutionCostCalculator costCalculator;

    /**
     * Size of partial download in MB.
     */
    @Value("${app.probe.partial-size-mb:10}")
    private int partialSizeMb;

    @Override
    public String getName() {
        return "PartialDownload";
    }

    @Override
    public boolean supports(String bucket, String key) {
        // Support MP4 files (moov atom usually at beginning)
        String lowerKey = key.toLowerCase();
        return lowerKey.endsWith(".mp4") || lowerKey.endsWith(".mov");
    }

    @Override
    public int getPriority() {
        return 50; // Medium priority - try after presigned URL
    }

    @Override
    public ProbeResult probe(String bucket, String key) throws ProbeException {
        log.debug("Probing {}/{} via partial download ({} MB)", bucket, key, partialSizeMb);

        Path tempFile = null;
        try {
            // Download partial content
            long partialBytes = partialSizeMb * 1024L * 1024L;
            byte[] partialData = downloadService.downloadPartial(bucket, key, partialBytes);

            // Write to temp file for FFprobe
            tempFile = Files.createTempFile("probe_", ".partial");
            Files.write(tempFile, partialData);

            // Probe temp file
            FFmpegVideoMetadata metadata = metadataService.getMetadata(tempFile);

            // Determine target resolutions
            List<VideoTranscoderService.VideoResolution> resolutions =
                    videoTranscoderService.determineTargetResolutions(metadata);

            // Calculate costs
            int peakCost = 0;
            int totalCost = 0;
            List<String> resolutionSuffixes = new ArrayList<>();

            for (var res : resolutions) {
                int cost = costCalculator.calculateCost(res.filename());
                peakCost = Math.max(peakCost, cost);
                totalCost += cost;
                resolutionSuffixes.add(res.filename());
            }

            log.info("Probed {}/{} (partial): {}x{}, resolutions={}, peakCost={}, totalCost={}",
                    bucket, key,
                    metadata.width(), metadata.height(),
                    resolutionSuffixes,
                    peakCost, totalCost);

            return ProbeResult.forVideo(
                    metadata.width(),
                    metadata.height(),
                    metadata.duration(),
                    metadata.codec(),
                    resolutionSuffixes,
                    peakCost,
                    totalCost
            );

        } catch (Exception e) {
            log.warn("Partial download probe failed for {}/{}: {}", bucket, key, e.getMessage());
            throw new ProbeException("Partial download probe failed: " + e.getMessage(), e);
        } finally {
            // Clean up temp file
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    log.warn("Failed to delete temp file: {}", tempFile);
                }
            }
        }
    }
}

