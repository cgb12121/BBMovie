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
public class StreamingService {

    private final MinioClient minioClient;
    private final StreamingAccessControlService accessControlService;

    @Value("${minio.bucket.hls}")
    private String hlsBucket;

    @Value("${minio.bucket.secure}")
    private String secureBucket;

    /**
     * Gets the filtered master playlist based on the user subscription tier.
     * Performs access control and filtering internally.
     *
     * @param movieId The movie ID
     * @param tierStr The user's subscription tier string
     * @return Filtered master playlist as a Resource
     */
    public Resource getFilteredMasterPlaylist(UUID movieId, String tierStr) {
        String originalContent = getMasterPlaylistContent(movieId);
        String filteredContent = accessControlService.filterMasterPlaylist(originalContent, tierStr);
        byte[] contentBytes = filteredContent.getBytes(StandardCharsets.UTF_8);
        return new ByteArrayResource(contentBytes);
    }

    /**
     * Gets the HLS playlist file for a specific resolution.
     * Performs access control check before serving the file.
     *
     * @param movieId    The movie ID
     * @param resolution The resolution string (e.g., "720p", "1080p")
     * @param tierStr    The user's subscription tier string
     * @return The playlist file as InputStreamResource
     */
    public InputStreamResource getHlsFile(UUID movieId, String resolution, String tierStr) {
        // Check access - throws AccessDeniedException if denied
        accessControlService.checkAccessToResolution(tierStr, resolution);
        
        String objectKey = "movies/" + movieId + "/" + resolution + "/playlist.m3u8";
        return getFile(hlsBucket, objectKey);
    }

    /**
     * Gets the encryption key file for a specific resolution.
     * Performs access control check before serving the file.
     *
     * @param movieId    The movie ID
     * @param resolution The resolution string (e.g., "720p", "1080p")
     * @param keyFile    The key file name (e.g., "key_001.key")
     * @param tierStr    The user's subscription tier string
     * @return The key file as InputStreamResource
     */
    public InputStreamResource getSecureKey(UUID movieId, String resolution, String keyFile, String tierStr) {
        // Check access - throws AccessDeniedException if denied
        accessControlService.checkAccessToResolution(tierStr, resolution);
        
        String objectKey = "movies/" + movieId + "/" + resolution + "/" + keyFile;
        return getFile(secureBucket, objectKey);
    }

    /**
     * Gets the master playlist content as a string from MinIO.
     * Internal method used for filtering.
     *
     * @param movieId The movie ID
     * @return The master playlist content as a string
     */
    private String getMasterPlaylistContent(UUID movieId) {
        String objectKey = "movies/" + movieId.toString() + "/master.m3u8";
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(hlsBucket)
                        .object(objectKey)
                        .build())) {
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Failed to fetch master playlist {} from bucket {}", objectKey, hlsBucket, e);
            throw new InaccessibleFileException("Master playlist not found or inaccessible: " + objectKey);
        }
    }

    /**
     * Internal method to retrieve a file from MinIO.
     *
     * @param bucket    The bucket name
     * @param objectKey The object key (path)
     * @return The file as InputStreamResource
     */
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
