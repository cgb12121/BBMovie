package com.bbmovie.transcodeworker.service.nats;

import com.bbmovie.transcodeworker.dto.MediaStatusUpdateEvent;
import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.service.ffmpeg.ImageProcessingService;
import com.bbmovie.transcodeworker.service.ffmpeg.MetadataService;
import com.bbmovie.transcodeworker.service.ffmpeg.VideoTranscoderService;
import com.bbmovie.transcodeworker.service.validation.ClamAVService;
import com.bbmovie.transcodeworker.service.validation.TikaValidationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service class that consumes media events from NATS messaging system.
 * This consumer listens for MinIO object creation events and processes uploaded files
 * based on their upload purpose (video transcoding, image processing, etc.).
 * It handles file validation, malware scanning, and processes files accordingly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaEventConsumer {

    /** Base temporary directory for processing files */
    @Value("${app.transcode.temp-dir}")
    private String baseTempDir;

    /** NATS connection for receiving events and publishing status updates */
    private final Connection natsConnection;
    /** MinIO client for accessing object storage */
    private final MinioClient minioClient;
    /** Object mapper for JSON serialization/deserialization */
    private final ObjectMapper objectMapper;
    /** Tika validation service for file type detection */
    private final TikaValidationService tikaValidationService;
    /** ClamAV service for malware scanning */
    private final ClamAVService clamAVService;
    /** Video transcoder service for video processing */
    private final VideoTranscoderService videoTranscoderService;
    /** Image processing service for image processing */
    private final ImageProcessingService imageProcessingService;
    /** Metadata service for extracting video metadata */
    private final MetadataService metadataService;

    /** Executor service that uses virtual threads for high-concurrency I/O tasks */
    // Use Virtual Threads for high-concurrency I/O tasks
    private final ExecutorService workerExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Initializes the NATS event consumer by creating a dispatcher and subscribing to the minio.events subject.
     * This method is called after dependency injection is completed.
     */
    @PostConstruct
    public void init() {
        Dispatcher dispatcher = natsConnection.createDispatcher(this::handleMessage);
        dispatcher.subscribe("minio.events");
    }

    /**
     * Handles incoming NATS messages containing MinIO events.
     * This method parses the JSON message and processes individual records in separate threads.
     *
     * @param msg the NATS message containing the MinIO event
     */
    private void handleMessage(Message msg) {
        try {
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            log.info("Received MinIO event: {}", json);
            JsonNode rootNode = objectMapper.readTree(json);

            if (rootNode.has("Records")) {
                for (JsonNode record : rootNode.get("Records")) {
                    // Offload processing to Virtual Thread
                    workerExecutor.submit(() -> processRecord(record));
                }
            } else if (rootNode.has("Key")) {
                 workerExecutor.submit(() -> processRecord(rootNode));
            }

        } catch (Exception e) {
            log.error("Error parsing NATS message", e);
        }
    }

    /**
     * Processes a single record from a MinIO event.
     * This method downloads the file from MinIO, validates it, and processes it based on its purpose.
     * It handles error cases and ensures cleanup of temporary files.
     *
     * @param record the JSON node containing the record information
     */
    private void processRecord(JsonNode record) {
        String uploadId = null;
        Path tempDir = null;
        try {
            String eventName = record.path("eventName").asText();
            if (!eventName.startsWith("s3:ObjectCreated:")) {
                return;
            }

            String bucket = record.path("s3").path("bucket").path("name").asText();
            String key = record.path("s3").path("object").path("key").asText();
            key = java.net.URLDecoder.decode(key, StandardCharsets.UTF_8);

            log.info("Processing file: bucket={}, key={}", bucket, key);

            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder().bucket(bucket).object(key).build());

            // Case-insensitive metadata lookup
            Map<String, String> meta = stat.userMetadata();
            String purposeStr = meta.entrySet().stream()
                    .filter(e -> "x-amz-meta-purpose".equalsIgnoreCase(e.getKey()) || "purpose".equalsIgnoreCase(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);

            if (purposeStr == null) {
                log.warn("No UploadPurpose found for object: {}. Skipping.", key);
                return;
            }

            UploadPurpose purpose = UploadPurpose.valueOf(purposeStr);

            uploadId = meta.entrySet().stream()
                    .filter(e -> "x-amz-meta-video-id".equalsIgnoreCase(e.getKey()) || "video-id".equalsIgnoreCase(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(UUID.randomUUID().toString());

            // Setup temp directory
            Path basePath = Path.of(baseTempDir);
            Files.createDirectories(basePath);
            tempDir = basePath.resolve("transcode_" + UUID.randomUUID());
            Files.createDirectories(tempDir);

            // Use safe filename
            String originalExt = com.google.common.io.Files.getFileExtension(key);
            if (originalExt.isEmpty()) originalExt = "bin";
            Path tempFile = tempDir.resolve("source." + originalExt);

            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(key).build())) {
                Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }


            log.info("Downloaded file to {}, size: {} bytes", tempFile, Files.size(tempFile));

            log.info("Starting validation...");
            tikaValidationService.validate(tempFile, purpose);
            boolean isClean = clamAVService.scanFile(tempFile);
            if (!isClean) {
                log.error("Malware detected in file: {}", key);
                publishStatus(uploadId, "REJECTED", "Malware detected");
                return;
            }

            log.info("Validation passed. Processing file...");
            processFile(tempFile, purpose, tempDir, uploadId);

            publishStatus(uploadId, "READY", null);
            log.info("Successfully processed for uploadId: {}", uploadId);

        } catch (Throwable e) {
            log.error("Error processing record for uploadId: {}", uploadId, e);
            if (uploadId != null) {
                publishStatus(uploadId, "FAILED", e.getMessage());
            }
        } finally {
            // Robust cleanup

            if (tempDir != null) {
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                    log.debug("Cleaned up temp dir: {}", tempDir);
                } catch (Exception e) {
                    log.warn("Failed to cleanup temp dir: {}", tempDir, e);
                }
            }
        }
    }

    /**
     * Processes the downloaded file based on its upload purpose.
     * For video files, it performs transcoding to multiple resolutions.
     * For image files, it generates different sizes according to the upload purpose.
     *
     * @param input the path to the downloaded input file
     * @param purpose the upload purpose that determines how to process the file
     * @param tempDir the temporary directory for processing
     * @param uploadId the unique identifier for the upload operation
     * @throws Exception if there's an issue during file processing
     */
    private void processFile(Path input, UploadPurpose purpose, Path tempDir, String uploadId) throws Exception {
        log.info("Starting processing for purpose: {}", purpose);

        if (purpose == UploadPurpose.MOVIE_SOURCE || purpose == UploadPurpose.MOVIE_TRAILER) {
             var metadata = metadataService.getMetadata(input);
             log.info("Metadata retrieved: {}", metadata);

             var resolutions = videoTranscoderService.determineTargetResolutions(metadata);
             log.info("Target resolutions: {}", resolutions);

             Path hlsOutputDir = tempDir.resolve("hls");
             Files.createDirectories(hlsOutputDir);

             log.info("Starting transcoding to: {}", hlsOutputDir);
             videoTranscoderService.transcode(input, resolutions, hlsOutputDir.toString(), uploadId);
             log.info("Transcoding finished. Uploading...");

             uploadDirectory(hlsOutputDir, "bbmovie-hls", "movies/" + uploadId);

        } else if (purpose == UploadPurpose.MOVIE_POSTER || purpose == UploadPurpose.USER_AVATAR) {
             Path imgOutputDir = tempDir.resolve("images");
             Files.createDirectories(imgOutputDir);

             String format = com.google.common.io.Files.getFileExtension(input.toString());
             if(format.isEmpty()) format = "jpg";

             // Pass purpose to handle sizing logic
             imageProcessingService.processImageHierarchy(input, imgOutputDir.toString(), format, purpose);

             String prefix = (purpose == UploadPurpose.USER_AVATAR ? "users/avatars/" : "movies/posters/") + uploadId;
             uploadDirectory(imgOutputDir, "bbmovie-public", prefix);
        }
    }

    /**
     * Determines the appropriate content type for a file based on its extension.
     * This method attempts to detect the content type using the system's file type detection
     * and falls back to extension-based mapping if detection fails.
     *
     * @param path the path to the file
     * @return the determined content type for the file
     */
    private String getContentType(Path path) {
        try {
            String contentType = Files.probeContentType(path);
            if (contentType != null) {
                return contentType;
            }
        } catch (Exception ignored) {
            // Ignore failure, fall back to extension
        }

        String filename = path.getFileName().toString().toLowerCase();
        if (filename.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (filename.endsWith(".ts")) return "video/MP2T";
        if (filename.endsWith(".key")) return "application/octet-stream";
        if (filename.endsWith(".mp4")) return "video/mp4";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".png")) return "image/png";

        return "application/octet-stream";
    }

    /**
     * Uploads all files in a directory to MinIO storage.
     * This method handles special file types like encryption keys that need to go to a secure bucket.
     * It excludes temporary and pattern files that should not be uploaded.
     *
     * @param dir the directory containing files to upload
     * @param bucket the target MinIO bucket name
     * @param prefix the prefix to use for object names in the bucket
     * @throws Exception if there's an issue during the upload process
     */
    private void uploadDirectory(Path dir, String bucket, String prefix) throws Exception {
        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucket).build());
        }

        // Ensure secure bucket exists for keys
        String secureBucket = "bbmovie-secure";
        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(secureBucket).build())) {
            minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(secureBucket).build());
        }

        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                    // QUAN TRỌNG: Loại bỏ file pattern và file tạm
                    .filter(path -> !path.getFileName().toString().contains("%"))
                    .filter(path -> !path.getFileName().toString().endsWith(".txt")) // Loại bỏ keyinfo.txt
                    .forEach(path -> {
                        try {
                            String relativePath = dir.relativize(path).toString().replace("\\", "/");
                            String objectName = prefix + "/" + relativePath;

                            String targetBucket = bucket;
                            if (path.toString().endsWith(".key")) {
                                targetBucket = secureBucket;
                            }

                            minioClient.putObject(
                                    io.minio.PutObjectArgs.builder()
                                            .bucket(targetBucket)
                                            .object(objectName)
                                            .stream(Files.newInputStream(path), Files.size(path), -1)
                                            .contentType(getContentType(path))
                                            .build());
                            log.info("Uploaded: {}/{}", targetBucket, objectName);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to upload file " + path, e);
                        }
                    });
        }
    }

    /**
     * Publishes a status update event to the NATS messaging system.
     * This method creates a MediaStatusUpdateEvent and publishes it to the media.status.update subject.
     *
     * @param uploadId the unique identifier for the upload operation
     * @param status the current status of the operation
     * @param reason the reason for the status (particularly for error statuses)
     */
    private void publishStatus(String uploadId, String status, String reason) {
        try {
            MediaStatusUpdateEvent event = MediaStatusUpdateEvent.builder()
                    .uploadId(uploadId)
                    .status(status)
                    .reason(reason)
                    .build();
            String json = objectMapper.writeValueAsString(event);
            natsConnection.publish("media.status.update", json.getBytes(StandardCharsets.UTF_8));
            log.info("Published status update: {} for {}", status, uploadId);
        } catch (Exception e) {
            log.error("Failed to publish status update", e);
        }
    }
}