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

/**
 * Service responsible for serving files from MinIO storage with access control.
 * This service orchestrates file retrieval and access control checks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyMediaStreamService {

    private final MinioClient minioClient;
    private final StreamingAccessControlService accessControlService;
    private final EntitlementClient entitlementClient;

    @Value("${minio.bucket.hls}")
    private String hlsBucket;

    @Value("${minio.bucket.secure}")
    private String secureBucket;

    public Resource getFilteredMasterPlaylist(UUID movieId, String userId) {
        String tierStr = entitlementClient.resolveTierOrDeny(userId, movieId, "STREAM");
        String originalContent = getMasterPlaylistText(movieId);
        String filteredContent = accessControlService.filterMasterPlaylist(originalContent, tierStr);
        byte[] contentBytes = filteredContent.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayResource(contentBytes);
    }

    /**
     * Raw master playlist text from MinIO (for direct/presigned flows that rewrite variant URIs).
     */
    public String getMasterPlaylistText(UUID movieId) {
        return readMasterPlaylistText(movieId);
    }

    public InputStreamResource getHlsFile(UUID movieId, String resolution, String userId) {
        String tierStr = entitlementClient.resolveTierOrDeny(userId, movieId, "STREAM");
        accessControlService.checkAccessToResolution(tierStr, resolution);
        String objectKey = "movies/" + movieId + "/" + resolution + "/playlist.m3u8";
        return getFile(hlsBucket, objectKey);
    }

    public InputStreamResource getSecureKey(UUID movieId, String resolution, String keyFile, String userId) {
        String tierStr = entitlementClient.resolveTierOrDeny(userId, movieId, "STREAM");
        accessControlService.checkAccessToResolution(tierStr, resolution);
        String objectKey = "movies/" + movieId + "/" + resolution + "/" + keyFile;
        return getFile(secureBucket, objectKey);
    }

    private String readMasterPlaylistText(UUID movieId) {
        String objectKey = "movies/" + movieId + "/master.m3u8";
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
}
