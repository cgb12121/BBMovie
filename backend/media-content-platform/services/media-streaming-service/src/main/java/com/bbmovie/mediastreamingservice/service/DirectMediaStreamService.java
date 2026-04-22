package com.bbmovie.mediastreamingservice.service;

import com.bbmovie.mediastreamingservice.exception.InaccessibleFileException;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Non-proxy streaming: tier filtering stays on the API; variant playlists, keys, and segments are
 * fetched by the client directly from MinIO using pre-signed URLs (no byte proxy through Spring).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DirectMediaStreamService {

    private static final Pattern RELATIVE_VARIANT_PLAYLIST = Pattern.compile(
            "^(?:144|240|360|480|720|1080|1440|2160|4080)p/playlist\\.m3u8$");

    private final MinioClient minioClient;
    private final StreamingAccessControlService accessControlService;
    private final ProxyMediaStreamService proxyMediaStreamService;
    private final EntitlementClient entitlementClient;

    @Value("${minio.bucket.hls}")
    private String hlsBucket;

    @Value("${minio.bucket.secure}")
    private String secureBucket;

    @Value("${streaming.direct.presigned-expiry-seconds:3600}")
    private int presignedExpirySeconds;

    /**
     * Filtered master playlist with each allowed variant line rewritten to a GET pre-signed URL
     * pointing at MinIO. Relative segment URLs inside variant playlists then resolve against MinIO.
     */
    public Resource getFilteredMasterPlaylistWithPresignedVariants(UUID movieId, String userId) {
        String tierStr = entitlementClient.resolveTierOrDeny(userId, movieId, "STREAM");
        String original = proxyMediaStreamService.getMasterPlaylistText(movieId);
        String filtered = accessControlService.filterMasterPlaylist(original, tierStr);
        String rewritten = rewriteVariantLinesToPresigned(movieId, filtered);
        return new ByteArrayResource(rewritten.getBytes(StandardCharsets.UTF_8));
    }

    public String presignResolutionPlaylistUrl(UUID movieId, String resolution, String userId) {
        String tierStr = entitlementClient.resolveTierOrDeny(userId, movieId, "STREAM");
        accessControlService.checkAccessToResolution(tierStr, resolution);
        String objectKey = "movies/" + movieId + "/" + resolution + "/playlist.m3u8";
        return presignGet(hlsBucket, objectKey);
    }

    public String presignSecureKeyUrl(UUID movieId, String resolution, String keyFile, String userId) {
        String tierStr = entitlementClient.resolveTierOrDeny(userId, movieId, "STREAM");
        accessControlService.checkAccessToResolution(tierStr, resolution);
        String objectKey = "movies/" + movieId + "/" + resolution + "/" + keyFile;
        return presignGet(secureBucket, objectKey);
    }

    private String rewriteVariantLinesToPresigned(UUID movieId, String playlist) {
        StringBuilder out = new StringBuilder();
        for (String line : playlist.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()
                    && !trimmed.startsWith("#")
                    && !trimmed.startsWith("http://")
                    && !trimmed.startsWith("https://")
                    && RELATIVE_VARIANT_PLAYLIST.matcher(trimmed).matches()) {
                String resolution = trimmed.substring(0, trimmed.indexOf('/'));
                String objectKey = "movies/" + movieId + "/" + resolution + "/playlist.m3u8";
                out.append(presignGet(hlsBucket, objectKey)).append('\n');
            } else {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    private String presignGet(String bucket, String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(presignedExpirySeconds, TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("Failed to presign GET for {}/{}", bucket, objectKey, e);
            throw new InaccessibleFileException("Failed to generate pre-signed URL: " + objectKey);
        }
    }
}
