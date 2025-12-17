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

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaEventConsumer {

    @Value("${app.transcode.temp-dir}")
    private String baseTempDir;

    private final Connection natsConnection;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;
    private final TikaValidationService tikaValidationService;
    private final ClamAVService clamAVService;
    private final VideoTranscoderService videoTranscoderService;
    private final ImageProcessingService imageProcessingService;
    private final MetadataService metadataService;

    // Use Virtual Threads for high concurrency I/O tasks
    private final ExecutorService workerExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @PostConstruct
    public void init() {
        Dispatcher dispatcher = natsConnection.createDispatcher(this::handleMessage);
        dispatcher.subscribe("minio.events");
    }

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

            tikaValidationService.validate(tempFile, purpose);
            boolean isClean = clamAVService.scanFile(tempFile);
            if (!isClean) {
                log.error("Malware detected in file: {}", key);
                publishStatus(uploadId, "REJECTED", "Malware detected");
                return;
            }

            processFile(tempFile, purpose, tempDir, uploadId);
            
            publishStatus(uploadId, "READY", null);
            log.info("Successfully processed for uploadId: {}", uploadId);

        } catch (Exception e) {
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

    private void processFile(Path input, UploadPurpose purpose, Path tempDir, String uploadId) throws Exception {
        log.info("Starting processing for purpose: {}", purpose);
        
        if (purpose == UploadPurpose.MOVIE_SOURCE || purpose == UploadPurpose.MOVIE_TRAILER) {
             var metadata = metadataService.getMetadata(input);
             var resolutions = videoTranscoderService.determineTargetResolutions(metadata);
             
             Path hlsOutputDir = tempDir.resolve("hls");
             Files.createDirectories(hlsOutputDir);
             
             videoTranscoderService.transcode(input, resolutions, hlsOutputDir.toString());
             
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

    private void uploadDirectory(Path dir, String bucket, String prefix) throws Exception {
        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucket).build());
        }

        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                  .forEach(path -> {
                      try {
                          String relativePath = dir.relativize(path).toString().replace("\\", "/");
                          String objectName = prefix + "/" + relativePath;
                          
                          minioClient.putObject(
                                  io.minio.PutObjectArgs.builder()
                                          .bucket(bucket)
                                          .object(objectName)
                                          .stream(Files.newInputStream(path), Files.size(path), -1)
                                          .contentType(probeContentType(path))
                                          .build());
                          log.info("Uploaded: {}/{}", bucket, objectName);
                      } catch (Exception e) {
                          throw new RuntimeException("Failed to upload file " + path, e);
                      }
                  });
        }
    }

    private String probeContentType(Path path) {
        try {
            return Files.probeContentType(path);
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }

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