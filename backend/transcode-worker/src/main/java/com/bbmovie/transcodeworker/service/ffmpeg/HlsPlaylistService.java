package com.bbmovie.transcodeworker.service.ffmpeg;

import com.bbmovie.transcodeworker.service.ffmpeg.VideoTranscoderService.VideoResolution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service responsible for HLS playlist management.
 * <p>
 * Handles:
 * - Master playlist creation
 * - Resolution playlist URL updates
 * - Bandwidth estimation
 * <p>
 * Extracted from VideoTranscoderService to follow the Single Responsibility Principle.
 */
@Slf4j
@Service
public class HlsPlaylistService {

    /** Base URL for public HLS content in MinIO storage */
    @Value("${app.minio.public-hls-url}")
    private String minioPublicUrl;

    /**
     * Creates a master playlist file that references all the generated resolution playlists.
     * <p>
     * This HLS master playlist allows clients to select from multiple quality streams.
     *
     * @param resolutions the list of resolutions that have been generated
     * @param outputDir   the output directory where the master playlist should be created
     */
    public void createMasterPlaylist(List<VideoResolution> resolutions, Path outputDir) {
        Path masterPath = outputDir.resolve("master.m3u8");

        StringBuilder content = new StringBuilder();
        content.append("#EXTM3U\n");
        content.append("#EXT-X-VERSION:6\n");

        for (VideoResolution res : resolutions) {
            long bandwidth = getEstimatedBandwidth(res);
            content.append(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,RESOLUTION=%dx%d\n",
                    bandwidth, res.width(), res.height()));
            content.append(String.format("%s/playlist.m3u8\n", res.name()));
        }

        try {
            Files.writeString(masterPath, content.toString());
            log.info("Generated master playlist at {}", masterPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write master playlist", e);
        }
    }

    /**
     * Updates URLs in the playlist file to point to the correct public locations.
     * <p>
     * FFmpeg replaces key URIs when using a key info file, but this method ensures
     * the segment URLs are updated to point to the public MinIO location.
     *
     * @param playlistPath the path to the playlist file to update
     * @param uploadId     the unique identifier for the upload operation
     * @param resolution   the resolution name for this HLS stream
     * @param keyCount     the number of keys used (for logging)
     */
    public void updatePlaylistUrls(Path playlistPath, String uploadId, String resolution, int keyCount) {
        try {
            String content = Files.readString(playlistPath);

            // PUBLIC segment URL pattern
            String publicSegmentBaseUrl = String.format("%s/%s/%s/", minioPublicUrl, uploadId, resolution);

            // DEBUG: Log playlist content before update
            log.debug("[{}] Playlist before update (first 300 chars): {}",
                    resolution, content.substring(0, Math.min(300, content.length())));

            // Replace segment URLs
            String segmentRegex = "seg_(\\d+)\\.ts";
            String segmentReplacement = publicSegmentBaseUrl + "seg_$1.ts";
            String updatedContent = content.replaceAll(segmentRegex, segmentReplacement);

            // Verify key URLs
            int keyUrlCount = countKeyUrls(updatedContent);
            log.debug("[{}] Found {} key URLs in playlist", resolution, keyUrlCount);

            Files.writeString(playlistPath, updatedContent);
            log.info("[{}] Updated playlist - Keys: {}, Segments: {}",
                    resolution, keyCount, publicSegmentBaseUrl);

        } catch (IOException e) {
            log.error("[{}] Failed to update playlist URLs", resolution, e);
            throw new RuntimeException("Failed to update playlist URLs", e);
        }
    }

    /**
     * Counts the number of key URLs in the playlist content.
     *
     * @param content the playlist content as a string
     * @return the number of key URLs found in the content
     */
    public int countKeyUrls(String content) {
        int count = 0;
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.contains("#EXT-X-KEY") && line.contains("URI=")) {
                count++;
            }
        }
        return count;
    }

    /**
     * Estimates the required bandwidth for a given video resolution based on common streaming standards.
     *
     * @param res the video resolution to estimate bandwidth for
     * @return the estimated bandwidth in bits per second
     */
    public long getEstimatedBandwidth(VideoResolution res) {
        if (res.height() >= 4320) return 35000000; // 8K: 35 Mbps
        if (res.height() >= 2160) return 15000000; // 4K: 15 Mbps
        if (res.height() >= 1440) return 10000000; // 1440p: 10 Mbps
        if (res.height() >= 1080) return 6000000;  // 1080p: 6 Mbps
        if (res.height() >= 720) return 3000000;   // 720p: 3 Mbps
        if (res.height() >= 480) return 1500000;   // 480p: 1.5 Mbps
        if (res.height() >= 360) return 800000;    // 360p: 800 Kbps
        if (res.height() >= 240) return 450000;    // 240p: 450 Kbps
        return 256000;                             // 144p and below: 256 Kbps
    }

    /**
     * Logs information about the generated files for a specific resolution.
     *
     * @param resolutionDir the directory containing the generated files for a specific resolution
     */
    public void logGeneratedFiles(Path resolutionDir) {
        if (!log.isTraceEnabled()) {
            return;
        }
        try {
            try (Stream<Path> stream = Files.list(resolutionDir)) {
                List<Path> files = stream.toList();
                log.trace("[{}] Generated files:", resolutionDir.getFileName());

                Map<String, List<Path>> grouped = files.stream()
                        .collect(Collectors.groupingBy(p -> {
                            String name = p.getFileName().toString();
                            if (name.endsWith(".ts")) return "segments";
                            if (name.endsWith(".key")) return "keys";
                            if (name.endsWith(".m3u8")) return "playlists";
                            return "others";
                        }));

                for (Map.Entry<String, List<Path>> entry : grouped.entrySet()) {
                    log.trace("  {}: {} files", entry.getKey(), entry.getValue().size());
                    if (entry.getKey().equals("keys")) {
                        entry.getValue().forEach(p -> {
                            try {
                                log.trace("    - {} ({} bytes)", p.getFileName(), Files.size(p));
                            } catch (IOException e) {
                                log.error("    - {} (size unknown)", p.getFileName());
                            }
                        });
                    }
                }
            }
        } catch (IOException e) {
            log.error("[{}] Failed to list files", resolutionDir, e);
        }
    }
}

