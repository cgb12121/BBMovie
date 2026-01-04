package com.bbmovie.mediastreamingservice.service;

import com.bbmovie.mediastreamingservice.exception.InaccessibleFileException;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.hls}")
    private String hlsBucket;

    @Value("${minio.bucket.secure}")
    private String secureBucket;

    public InputStreamResource getHlsFile(UUID movieId, String resolution) {
        String objectKey = "movies/" + movieId + "/" + resolution + "/playlist.m3u8";
        return getFile(hlsBucket, objectKey);
    }

    public InputStreamResource getMasterPlaylist(UUID movieId) {
        String objectKey = "movies/" + movieId.toString() + "/master.m3u8";
        return getFile(hlsBucket, objectKey);
    }

    /**
     * Gets the master playlist content filtered based on user subscription tier.
     *
     * @param movieId The movie ID
     * @param tier    The user's subscription tier (FREE, STANDARD, PREMIUM)
     * @return Filtered master playlist as a Resource
     */
    public Resource getFilteredMasterPlaylist(UUID movieId, String tier) {
        String originalContent = getMasterPlaylistContent(movieId);
        String filteredContent = filterPlaylistByTier(originalContent, tier);
        byte[] contentBytes = filteredContent.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayResource(contentBytes);
    }

    private String getMasterPlaylistContent(UUID movieId) {
        String objectKey = "movies/" + movieId.toString() + "/master.m3u8";
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(hlsBucket)
                        .object(objectKey)
                        .build())) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to fetch master playlist {} from bucket {}", objectKey, hlsBucket, e);
            throw new InaccessibleFileException("Master playlist not found or inaccessible: " + objectKey);
        }
    }

    /**
     * Checks if a user tier has access to a specific resolution.
     *
     * @param tier       The user's subscription tier
     * @param resolution The resolution string (e.g., "720p", "1080p")
     * @return true if the user has access to this resolution
     */
    public boolean hasAccessToResolution(String tier, String resolution) {
        if ("PREMIUM".equalsIgnoreCase(tier)) {
            return true; // Premium users have access to all resolutions
        }

        if ("FREE".equalsIgnoreCase(tier)) {
            // FREE users only have access to 144p, 240p, 360p, 480p
            return resolution.equals("144p") ||
                   resolution.equals("240p") ||
                   resolution.equals("360p") ||
                   resolution.equals("480p");
        }

        if ("PREMIUM".equalsIgnoreCase(tier)) {
            // STANDARD users have access up to 1080p
            return resolution.equals("144p") ||
                   resolution.equals("240p") ||
                   resolution.equals("360p") ||
                   resolution.equals("480p") ||
                   resolution.equals("720p") ||
                   resolution.equals("1080p");
        }

        // Unknown tier - default to FREE restrictions
        return hasAccessToResolution("FREE", resolution);
    }

    public InputStreamResource getSecureKey(UUID movieId, String resolution, String keyFile) {
        String objectKey = "movies/" + movieId + "/" + resolution + "/" + keyFile;
        return getFile(secureBucket, objectKey);
    }

    private InputStreamResource getFile(String bucket, String objectKey) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build());
            return new InputStreamResource(stream);
        } catch (Exception e) {
            log.error("Failed to fetch file {} from bucket {}", objectKey, bucket, e);
            throw new InaccessibleFileException("File not found or inaccessible: " + objectKey);
        }
    }

    /**
     * Filters the master playlist content based on user subscription tier.
     * Removes premium resolutions (720p+) for non-premium users.
     *
     * @param content The original master playlist content
     * @param tier    The user's subscription tier (FREE, STANDARD, PREMIUM)
     * @return Filtered playlist content
     */
    private String filterPlaylistByTier(String content, String tier) {
        // PREMIUM users get full access
        if ("PREMIUM".equalsIgnoreCase(tier)) {
            return content;
        }

        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n");
        boolean skipNextUrl = false;

        for (String line : lines) {
            if (line.startsWith("#EXT-X-STREAM-INF")) {
                // Check if this stream should be filtered based on tier
                if (shouldFilterStream(line, tier)) {
                    skipNextUrl = true; // Mark to skip the URL line that follows
                    continue; // Skip this stream info line
                }
                skipNextUrl = false; // Reset if this stream is allowed
            } else if (!line.startsWith("#") && skipNextUrl) {
                // This is the URL line for a filtered stream - skip it
                continue;
            }

            result.append(line).append("\n");
        }

        return result.toString();
    }

    /**
     * Determines if a stream should be filtered based on tier and resolution.
     *
     * @param streamInfoLine The #EXT-X-STREAM-INF line containing resolution info
     * @param tier          The user's subscription tier
     * @return true if the stream should be filtered out
     */
    private boolean shouldFilterStream(String streamInfoLine, String tier) {
        // FREE tier: only allow up to 480p (854x480 or lower)
        if ("FREE".equalsIgnoreCase(tier)) {
            // Filter out 720p, 1080p, 1440p, 2160p, 4080p
            return streamInfoLine.contains("720") ||
                   streamInfoLine.contains("1080") ||
                   streamInfoLine.contains("1440") ||
                   streamInfoLine.contains("2160") ||
                   streamInfoLine.contains("4080") ||
                   // Also check for resolution format like "1280x720", "1920x1080", etc.
                   streamInfoLine.matches(".*RESOLUTION=\\d+x(?:720|1080|1440|2160|4080).*");
        }

        // STANDARD tier: allow up to 1080p, filter out 1440p, 2160p, 4080p
        if ("STANDARD".equalsIgnoreCase(tier)) {
            return streamInfoLine.contains("1440") ||
                   streamInfoLine.contains("2160") ||
                   streamInfoLine.contains("4080") ||
                   streamInfoLine.matches(".*RESOLUTION=\\d+x(?:1440|2160|4080).*");
        }

        // Unknown tier - default to FREE restrictions
        return shouldFilterStream(streamInfoLine, "FREE");
    }
}
