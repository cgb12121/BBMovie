package com.bbmovie.transcodeworker.service.storage;

import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Service for downloading files from MinIO.
 * Provides both full download and partial download capabilities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioDownloadService {

    private final MinioClient minioClient;

    /**
     * Downloads an object to a local file.
     *
     * @param bucket     Bucket name
     * @param key        Object key
     * @param targetPath Local path to save the file
     * @throws IOException If download fails
     */
    public void downloadToFile(String bucket, String key, Path targetPath) throws IOException {
        log.trace("Downloading {}/{} to {}", bucket, key, targetPath);

        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build())) {

            Files.copy(response, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Downloaded {}/{} to {} ({} bytes)", bucket, key, targetPath, Files.size(targetPath));
        } catch (Exception e) {
            log.error("Failed to download {}/{}", bucket, key, e);
            throw new IOException("Failed to download from MinIO", e);
        }
    }

    /**
     * Gets an InputStream for an object.
     * Caller is responsible for closing the stream.
     *
     * @param bucket Bucket name
     * @param key    Object key
     * @return InputStream for the object
     */
    public InputStream getInputStream(String bucket, String key) {
        try {
            log.trace("Getting input stream for {}/{}", bucket, key);

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );

        } catch (Exception e) {
            log.error("Failed to get input stream for {}/{}", bucket, key, e);
            throw new RuntimeException("Failed to get input stream from MinIO", e);
        }
    }

    /**
     * Downloads partial content (first N bytes) of an object.
     * Useful for probing file headers without a full download.
     *
     * @param bucket    Bucket name
     * @param key       Object key
     * @param maxBytes  Maximum bytes to download
     * @return Byte array with partial content
     */
    public byte[] downloadPartial(String bucket, String key, long maxBytes) {
        log.trace("Downloading partial content {}/{} (max {} bytes)", bucket, key, maxBytes);

        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .offset(0L)
                        .length(maxBytes)
                        .build())) {

            byte[] data = response.readAllBytes();
            log.debug("Downloaded {} bytes (partial) from {}/{}", data.length, bucket, key);
            return data;

        } catch (Exception e) {
            log.error("Failed to download partial content from {}/{}", bucket, key, e);
            throw new RuntimeException("Failed to download partial content", e);
        }
    }

    /**
     * Gets object metadata (size, content type, etc.) without downloading content.
     *
     * @param bucket Bucket name
     * @param key    Object key
     * @return Object statistics
     */
    public StatObjectResponse statObject(String bucket, String key) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to stat object {}/{}", bucket, key, e);
            throw new RuntimeException("Failed to stat object", e);
        }
    }

    /**
     * Gets the size of an object.
     *
     * @param bucket Bucket name
     * @param key    Object key
     * @return Object size in bytes
     */
    public long getObjectSize(String bucket, String key) {
        return statObject(bucket, key).size();
    }

    /**
     * Gets the content type of the object.
     *
     * @param bucket Bucket name
     * @param key    Object key
     * @return Content type string
     */
    public String getContentType(String bucket, String key) {
        return statObject(bucket, key).contentType();
    }

    /**
     * Checks if an object exists.
     *
     * @param bucket Bucket name
     * @param key    Object key
     * @return true if an object exists, false otherwise
     */
    public boolean exists(String bucket, String key) {
        try {
            statObject(bucket, key);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

