package com.bbmovie.transcodeworker.service.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.UploadObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Service for uploading files to MinIO.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioUploadService {

    private final MinioClient minioClient;

    /**
     * Uploads a local file to MinIO.
     *
     * @param bucket      Target bucket
     * @param key         Target object key
     * @param sourcePath  Local file path
     * @param contentType Content type of the file
     */
    public void uploadFile(String bucket, String key, Path sourcePath, String contentType) {
        try {
            log.trace("Uploading {} to {}/{}", sourcePath, bucket, key);

            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .filename(sourcePath.toString())
                            .contentType(contentType)
                            .build()
            );

            log.debug("Uploaded {} to {}/{} ({} bytes)", sourcePath, bucket, key, Files.size(sourcePath));
        } catch (Exception e) {
            log.error("Failed to upload {} to {}/{}", sourcePath, bucket, key, e);
            throw new RuntimeException("Failed to upload to MinIO", e);
        }
    }

    /**
     * Uploads content from an InputStream to MinIO.
     *
     * @param bucket      Target bucket
     * @param key         Target object key
     * @param inputStream Input stream with content
     * @param size        Size of content in bytes
     * @param contentType Content type of the content
     */
    public void uploadStream(String bucket, String key, InputStream inputStream, long size, String contentType) {
        try {
            log.trace("Uploading stream to {}/{} ({} bytes)", bucket, key, size);

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(key)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );

            log.debug("Uploaded stream to {}/{} ({} bytes)", bucket, key, size);

        } catch (Exception e) {
            log.error("Failed to upload stream to {}/{}", bucket, key, e);
            throw new RuntimeException("Failed to upload stream to MinIO", e);
        }
    }

    /**
     * Uploads a file with an auto-detected content type based on extension.
     *
     * @param bucket     Target bucket
     * @param key        Target object key
     * @param sourcePath Local file path
     */
    public void uploadFile(String bucket, String key, Path sourcePath) {
        String contentType = detectContentType(sourcePath);
        uploadFile(bucket, key, sourcePath, contentType);
    }

    /**
     * Uploads an entire directory to MinIO, preserving structure.
     *
     * @param sourceDir     Local directory to upload
     * @param bucket        Target bucket
     * @param destKeyPrefix Prefix for all uploaded objects (e.g., "movies/abc123")
     */
    public void uploadDirectory(Path sourceDir, String bucket, String destKeyPrefix) {
        log.trace("Uploading directory {} to {}/{}", sourceDir, bucket, destKeyPrefix);
        AtomicInteger fileCount = new AtomicInteger(0);

        try (Stream<Path> paths = Files.walk(sourceDir)) {
            paths.filter(Files::isRegularFile)
                    .forEach(file -> {
                        String relativePath = sourceDir.relativize(file).toString()
                                .replace("\\", "/"); // Windows path fix
                        String objectKey = destKeyPrefix + "/" + relativePath;
                        uploadFile(bucket, objectKey, file);
                        fileCount.incrementAndGet();
                    });

            log.debug("Uploaded {} files from {} to {}/{}", fileCount.get(), sourceDir, bucket, destKeyPrefix);

        } catch (IOException e) {
            log.error("Failed to walk directory: {}", sourceDir, e);
            throw new RuntimeException("Failed to upload directory to MinIO", e);
        }
    }

    /**
     * Detects content type based on file extension.
     */
    private String detectContentType(Path path) {
        String filename = path.getFileName().toString().toLowerCase();

        if (filename.endsWith(".mp4")) return "video/mp4";
        if (filename.endsWith(".webm")) return "video/webm";
        if (filename.endsWith(".m3u8")) return "application/x-mpegURL";
        if (filename.endsWith(".ts")) return "video/MP2T";
        if (filename.endsWith(".key")) return "application/octet-stream";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".webp")) return "image/webp";

        return "application/octet-stream";
    }
}

