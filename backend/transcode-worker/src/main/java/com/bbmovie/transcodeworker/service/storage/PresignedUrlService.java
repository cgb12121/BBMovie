package com.bbmovie.transcodeworker.service.storage;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for generating presigned URLs for MinIO objects.
 * <p>
 * Presigned URLs allow direct access to objects without authentication,
 * useful for FFprobe to analyze files without a full download.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PresignedUrlService {

    private final MinioClient minioClient;

    /**
     * Default expiry time for presigned URLs (in minutes).
     */
    @Value("${app.minio.presigned-url-expiry-minutes:60}")
    private int defaultExpiryMinutes;

    /**
     * Generates a presigned GET URL for an object.
     * <p>
     * The URL allows temporary read access to the object.
     *
     * @param bucket         Bucket name
     * @param key            Object key
     * @param expiryMinutes  URL expiry time in minutes
     * @return Presigned URL string
     */
    public String generateGetUrl(String bucket, String key, int expiryMinutes) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(key)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );

            log.debug("Generated presigned URL for {}/{} (expires in {} minutes)", bucket, key, expiryMinutes);
            return url;

        } catch (Exception e) {
            log.error("Failed to generate presigned URL for {}/{}", bucket, key, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    /**
     * Generates a presigned GET URL with default expiry.
     *
     * @param bucket Bucket name
     * @param key    Object key
     * @return Presigned URL string
     */
    public String generateGetUrl(String bucket, String key) {
        return generateGetUrl(bucket, key, defaultExpiryMinutes);
    }

    /**
     * Generates a short-lived presigned URL for probing (5 minutes).
     * <p>
     * Shorter expiry for security since probing should be quick.
     *
     * @param bucket Bucket name
     * @param key    Object key
     * @return Presigned URL string
     */
    public String generateProbeUrl(String bucket, String key) {
        return generateGetUrl(bucket, key, 5);
    }
}

