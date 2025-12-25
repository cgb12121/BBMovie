package com.bbmovie.transcodeworker.service.probe.strategy;

import com.bbmovie.transcodeworker.service.ffmpeg.FFmpegVideoMetadata;
import com.bbmovie.transcodeworker.service.ffmpeg.MetadataService;
import com.bbmovie.transcodeworker.service.ffmpeg.VideoTranscoderService;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeResult;
import com.bbmovie.transcodeworker.service.scheduler.ResolutionCostCalculator;
import com.bbmovie.transcodeworker.service.storage.PresignedUrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Probing strategy using presigned URLs.
 * <p>
 * This is the preferred strategy as it:
 * - Does not require file download
 * - FFprobe reads directly from URL
 * - Fast for files with metadata at the beginning
 * <p>
 * Limitations:
 * - Requires MinIO to be accessible from FFprobe network
 * - May fail for some file formats with metadata at the end
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PresignedUrlProbeStrategy implements ProbeStrategy {

    private final PresignedUrlService presignedUrlService;
    private final MetadataService metadataService;
    private final VideoTranscoderService videoTranscoderService;
    private final ResolutionCostCalculator costCalculator;

    @Override
    public String getName() {
        return "PresignedUrl";
    }

    @Override
    public boolean supports(String bucket, String key) {
        // Support all video files
        String lowerKey = key.toLowerCase();
        return lowerKey.endsWith(".mp4") ||
                lowerKey.endsWith(".mkv") ||
                lowerKey.endsWith(".mov") ||
                lowerKey.endsWith(".webm") ||
                lowerKey.endsWith(".avi");
    }

    @Override
    public int getPriority() {
        return 100; // Highest priority - try this first
    }

    @Override
    public ProbeResult probe(String bucket, String key) throws ProbeException {
        log.debug("Probing {}/{} via presigned URL", bucket, key);

        try {
            // Generate presigned URL
            String url = presignedUrlService.generateProbeUrl(bucket, key);

            // Probe using FFprobe with URL
            FFmpegVideoMetadata metadata = metadataService.getMetadataFromUrl(url);

            // Determine target resolutions
            List<VideoTranscoderService.VideoResolution> resolutions =
                    videoTranscoderService.determineTargetResolutions(metadata);

            // Calculate costs
            int peakCost = 0;
            int totalCost = 0;
            List<String> resolutionSuffixes = new java.util.ArrayList<>();

            for (var res : resolutions) {
                int cost = costCalculator.calculateCost(res.filename());
                peakCost = Math.max(peakCost, cost);
                totalCost += cost;
                resolutionSuffixes.add(res.filename());
            }

            log.info("Probed {}/{}: {}x{}, duration={}s, resolutions={}, peakCost={}, totalCost={}",
                    bucket, key,
                    metadata.width(), metadata.height(),
                    String.format("%.2f", metadata.duration()),
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
            log.warn("Presigned URL probe failed for {}/{}: {}", bucket, key, e.getMessage());
            throw new ProbeException("Presigned URL probe failed: " + e.getMessage(), e);
        }
    }
}

