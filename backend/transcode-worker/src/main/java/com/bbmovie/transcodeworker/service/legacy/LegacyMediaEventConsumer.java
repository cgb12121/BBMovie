package com.bbmovie.transcodeworker.service.legacy;

import com.bbmovie.transcodeworker.dto.MediaStatusUpdateEvent;
import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.service.ffmpeg.ImageProcessingService;
import com.bbmovie.transcodeworker.service.ffmpeg.MetadataService;
import com.bbmovie.transcodeworker.service.ffmpeg.VideoTranscoderService;
import com.bbmovie.transcodeworker.service.scheduler.TranscodeScheduler;
import com.bbmovie.transcodeworker.service.validation.ClamAVService;
import com.bbmovie.transcodeworker.service.validation.TikaValidationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.JetStreamManagement;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PullSubscribeOptions;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * LEGACY: Original monolithic media event consumer.
 * <p>
 * This class is DEPRECATED and kept for backward compatibility.
 * Use the new 3-Stage Pipeline (PipelineOrchestrator) instead by setting:
 * <pre>
 * app.pipeline.enabled=true
 * </pre>
 * <p>
 * Issues with this class:
 * - Does too many things (violates Single Responsibility Principle)
 * - Duplicate MetadataService calls
 * - No early cost discovery (Chicken-and-Egg problem)
 * - Head-of-line blocking potential
 * <p>
 * This consumer is only loaded when app.pipeline.enabled=false (default).
 * 
 * @deprecated Use {@link com.bbmovie.transcodeworker.service.pipeline.PipelineOrchestrator} instead
 * @see com.bbmovie.transcodeworker.service.pipeline.PipelineOrchestrator
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Deprecated(since = "2025-12-25", forRemoval = true)
@ConditionalOnProperty(name = "app.pipeline.enabled", havingValue = "false", matchIfMissing = true)
public class LegacyMediaEventConsumer {

    /** Base temporary directory for processing files */
    @Value("${app.transcode.temp-dir}")
    private String baseTempDir;

    /** MinIO bucket names */
    @Value("${app.minio.hls-bucket}")
    private String hlsBucket;

    @Value("${app.minio.public-bucket}")
    private String publicBucket;

    @Value("${app.minio.secure-bucket}")
    private String secureBucket;

    private final Connection natsConnection;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;
    private final TikaValidationService tikaValidationService;
    private final ClamAVService clamAVService;
    private final VideoTranscoderService videoTranscoderService;
    private final ImageProcessingService imageProcessingService;
    private final MetadataService metadataService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /** Subject to subscribe to */
    @Value("${nats.minio.subject:minio.events}")
    private String minioSubject;
    
    /** Stream name for JetStream */
    @Value("${nats.stream.name:BBMOVIE}")
    private String streamName;
    
    /** Consumer durable name */
    @Value("${nats.consumer.durable:transcode-worker}")
    private String consumerDurable;
    
    /** Ack wait duration (how long to wait before redelivery) */
    @Value("${nats.consumer.ack-wait-minutes:5}")
    private int ackWaitMinutes;
    
    /** Heartbeat interval in seconds */
    @Value("${nats.consumer.heartbeat-interval-seconds:30}")
    private int heartbeatIntervalSeconds;
    
    /** Batch size for fetching messages */
    @Value("${nats.consumer.fetch-batch-size:5}")
    private int fetchBatchSize;
    
    /** Fetch timeout in seconds */
    @Value("${nats.consumer.fetch-timeout-seconds:2}")
    private int fetchTimeoutSeconds;
    
    /** Transcode scheduler for resource management */
    private final TranscodeScheduler transcodeScheduler;

    /**
     * Initializes the NATS JetStream consumer using a Pull Subscription pattern.
     * This ensures flow control with max_ack_pending = maxCapacity.
     * Uses a heartbeat mechanism to prevent timeout during long-running transcoding.
     */
    @PostConstruct
    public void init() {
        log.warn("=== LEGACY MediaEventConsumer is being used ===");
        log.warn("Consider migrating to the new 3-Stage Pipeline by setting app.pipeline.enabled=true");
        
        try {
            // Get JetStreamManagement for stream/consumer management
            JetStreamManagement jsm = natsConnection.jetStreamManagement();
            
            // Get JetStream for subscription
            JetStream js = natsConnection.jetStream();
            
            // Ensure stream exists
            ensureStreamExists(jsm);
            
            // Setup consumer with max_ack_pending = maxCapacity
            setupConsumer(jsm);
            
            // Start worker loop
            startWorkerLoop(js);
            
            log.info("NATS Pull Subscription worker started. Subject: {}, Consumer: {}", minioSubject, consumerDurable);
        } catch (Exception e) {
            log.error("Failed to initialize NATS consumer", e);
            throw new RuntimeException("Failed to initialize NATS consumer", e);
        }
    }
    
    /**
     * Ensures the JetStream stream exists, creates it if not.
     * Uses JetStreamManagement API for stream management operations.
     */
    private void ensureStreamExists(JetStreamManagement jsm) throws Exception {
        try {
            StreamInfo streamInfo = jsm.getStreamInfo(streamName);
            log.info("Stream '{}' already exists, {}", streamName, streamInfo);
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                // Stream doesn't exist, create it
                log.info("Creating stream '{}' with subject '{}'", streamName, minioSubject);
                StreamConfiguration streamConfig = StreamConfiguration.builder()
                        .name(streamName)
                        .subjects(minioSubject)
                        .storageType(StorageType.File)
                        .build();
                jsm.addStream(streamConfig);
                log.info("Stream '{}' created successfully", streamName);
            } else {
                log.error("Error checking stream '{}': {}", streamName, e.getMessage(), e);
                throw e;
            }
        } catch (Exception e) {
            log.error("Unexpected error checking stream '{}'", streamName, e);
            throw e;
        }
    }
    
    /**
     * Sets up or updates the consumer with max_ack_pending = maxCapacity.
     * This allows NATS to send multiple messages (up to maxCapacity) so that
     * the scheduler can efficiently allocate resources based on cost weights.
     * <p>
     * Example: If maxCapacity=14, NATS can send 14 messages.
     * The scheduler will
     * then allocate slots based on cost (144p=1, 4 K=64, etc.), allowing parallel
     * processing of multiple small jobs while large jobs wait for available slots.
     * <p>
     * Uses JetStreamManagement API for consumer management operations.
     */
    private void setupConsumer(JetStreamManagement jsm) {
        try {
            // max_ack_pending should equal maxCapacity to allow parallel processing
            int maxAckPending = transcodeScheduler.getMaxCapacity();
            
            ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
                    .durable(consumerDurable)
                    .ackWait(Duration.ofMinutes(ackWaitMinutes))
                    .maxAckPending(maxAckPending) // KEY: Allow multiple messages = maxCapacity
                    .deliverPolicy(DeliverPolicy.All)
                    .build();
            
            // Try to add or update consumer
            jsm.addOrUpdateConsumer(streamName, consumerConfig);
            log.info("Consumer '{}' configured with max_ack_pending={} (maxCapacity), ack_wait={} minutes", 
                    consumerDurable, maxAckPending, ackWaitMinutes);
        } catch (JetStreamApiException e) {
            if (e.getErrorCode() == 404) {
                log.warn("Stream '{}' not found when setting up consumer. Stream should be created first.", streamName);
            } else {
                log.warn("Failed to setup consumer (may already exist): {}", e.getMessage());
            }
            // Consumer might already exist, that's OK
        } catch (Exception e) {
            log.warn("Failed to setup consumer: {}", e.getMessage());
            // Consumer might already exist, that's OK
        }
    }
    
    /**
     * Starts the worker loop that pulls messages in batches and processes them in parallel.
     * <p>
     * Key design:
     * - Fetches messages in batches (fetchBatchSize)
     * - Each message is processed in a separate virtual thread
     * - Each thread calls scheduler.acquire() which may block
     * - Heartbeat is sent from each thread to prevent timeout
     * - This allows parallel processing: multiple small jobs (144p) can run
     *   concurrently while large jobs (4 K) wait for available slots
     */
    private void startWorkerLoop(JetStream js) {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        
        executor.submit(() -> {
            try {
                // Create a pull subscription
                PullSubscribeOptions pullOptions = PullSubscribeOptions.builder()
                        .durable(consumerDurable)
                        .build();
                
                JetStreamSubscription sub = js.subscribe(minioSubject, pullOptions);
                log.info("Subscribed to '{}' with pull subscription. Max pending: {}, Batch size: {}", 
                        minioSubject, transcodeScheduler.getMaxCapacity(), fetchBatchSize);
                
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // Fetch batch of messages (non-blocking if no messages)
                        // NATS will only send it up to max_ack_pending messages
                        List<Message> messages = sub.fetch(fetchBatchSize, Duration.ofSeconds(fetchTimeoutSeconds));
                        
                        if (messages == null || messages.isEmpty()) {
                            continue; // No message, loop again
                        }
                        
                        log.debug("Fetched {} messages from NATS", messages.size());
                        
                        // Process each message in a separate virtual thread
                        // Main thread continues immediately to fetch more messages
                        for (Message msg : messages) {
                            // Offload to virtual thread - the main thread doesn't block
                            executor.submit(() -> processMessageWithHeartbeat(msg));
                        }
                        
                    } catch (Exception e) {
                        log.error("Error in worker loop", e);
                        // Continue loop on error
                    }
                }
            } catch (Exception e) {
                log.error("Fatal error in worker loop", e);
            }
        });
    }

    /**
     * Processes a NATS message with heartbeat mechanism and scheduler integration.
     * <p>
     * Flow:
     * 1. Start a heartbeat thread (sends inProgress() every N seconds)
     * 2. Parse message to extract records
     * 3. For each record, determine resolution and calculate cost
     * 4. Call scheduler.acquire(cost) - MAY BLOCK if no slots are available
     * 5. Process record (transcode)
     * 6. Release scheduler
     * 7. ACK message
     * <p>
     * This allows:
     * - Multiple small jobs (144p, cost=1) to run in parallel
     * - Large jobs (4 K, cost=64) to wait for available slots
     * - Efficient resource utilization based on cost weights
     *
     * @param msg the NATS message to process
     */
    private void processMessageWithHeartbeat(Message msg) {
        ScheduledFuture<?> heartbeatTask = null;

        try {
            // Start heartbeat: run immediately, repeat after an interval
            heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
                try {
                    msg.inProgress(); // Reset NATS timeout
                    log.debug("Sent inProgress heartbeat to NATS (SID: {})", msg.getSID());
                } catch (Exception e) {
                    log.warn("Error sending heartbeat", e);
                }
            }, 0, heartbeatIntervalSeconds, TimeUnit.SECONDS);

            // --- Main logic ---
            String json = new String(msg.getData(), StandardCharsets.UTF_8);
            log.info("Processing MinIO event (SID: {}): {}", msg.getSID(), json);
            JsonNode rootNode = objectMapper.readTree(json);

            if (rootNode.has("Records")) {
                for (JsonNode record : rootNode.get("Records")) {
                    processRecordWithScheduler(record);
                }
            } else if (rootNode.has("Key")) {
                processRecordWithScheduler(rootNode);
            }

            msg.ack();
            log.info("Message processed and ACKed successfully (SID: {})", msg.getSID());

        } catch (Exception e) {
            log.error("Error processing message (SID: {})", msg.getSID(), e);
            try {
                msg.nak();
            } catch (Exception nakError) {
                log.error("Failed to NAK message", nakError);
            }
        } finally {
            if (heartbeatTask != null) {
                heartbeatTask.cancel(true);
            }
        }
    }
    
    /**
     * Processes a record with scheduler integration.
     * <p>
     * Important: Scheduler resources are acquired INSIDE videoTranscoderService.transcode()
     * for each resolution, not here.
     * This allows:
     * - Multiple resolutions to be processed in parallel
     * - Each resolution to acquire resources based on its cost (144p=1, 4 K=64, etc.)
     * - Efficient resource utilization: small jobs can run while large jobs wait
     * <p>
     * This method handles:
     * - Downloading a file from MinIO
     * - Validation (Tika, ClamAV)
     * - Calling processFile() which internally calls videoTranscoderService.transcode()
     * <p>
     * The scheduler.acquire() calls happen inside transcode() for each resolution,
     * allowing multiple threads to block at scheduler level, enabling parallel processing.
     *
     * @param record the JSON node containing the record information
     */
    private void processRecordWithScheduler(JsonNode record) {
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

            // Extract metadata
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

            // Download file
            String originalExt = com.google.common.io.Files.getFileExtension(key);
            if (originalExt.isEmpty()) originalExt = "bin";
            Path tempFile = tempDir.resolve("source." + originalExt);

            try (InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(key).build())) {
                Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Downloaded file to {}, size: {} bytes", tempFile, Files.size(tempFile));

            // Validation
            log.info("Starting validation...");
            tikaValidationService.validate(tempFile, purpose);
            boolean isClean = clamAVService.scanFile(tempFile);
            if (!isClean) {
                log.error("Malware detected in file: {}", key);
                publishStatus(uploadId, "REJECTED", "Malware detected", null);
                return;
            }

            // Process file
            // Note: For video files,
            // scheduler.acquire() is called inside videoTranscoderService.transcode()
            // for each resolution.
            // This allows parallel processing of multiple resolutions.
            // For images, no scheduler is needed (lightweight).
            log.info("Validation passed. Processing file...");
            Double videoDuration = processFile(tempFile, purpose, tempDir, uploadId);

            publishStatus(uploadId, "READY", null, videoDuration);
            log.info("Successfully processed for uploadId: {}", uploadId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Processing interrupted for uploadId: {}", uploadId);
        } catch (Throwable e) {
            log.error("Error processing record for uploadId: {}", uploadId, e);
            if (uploadId != null) {
                publishStatus(uploadId, "FAILED", e.getMessage(), null);
            }
        } finally {
            // Note: Scheduler resources are released inside videoTranscoderService.transcode()
            // for each resolution.
            // No need to release here.
            
            // Cleanup temp directory
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
     * @return the duration of the video in seconds (null for non-video files)
     * @throws Exception if there's an issue during file processing
     */
    private Double processFile(Path input, UploadPurpose purpose, Path tempDir, String uploadId) throws Exception {
        log.info("Starting processing for purpose: {}", purpose);

        Double videoDuration = null;

        if (purpose == UploadPurpose.MOVIE_SOURCE || purpose == UploadPurpose.MOVIE_TRAILER) {
             var metadata = metadataService.getMetadata(input);
             log.info("Metadata retrieved: {}", metadata);
             
             // Store duration for event publishing
             videoDuration = metadata.duration();

             var resolutions = videoTranscoderService.determineTargetResolutions(metadata);
             log.info("Target resolutions: {}", resolutions);

             Path hlsOutputDir = tempDir.resolve("hls");
             Files.createDirectories(hlsOutputDir);

             log.info("Starting transcoding to: {}", hlsOutputDir);
             videoTranscoderService.transcode(input, resolutions, hlsOutputDir.toString(), uploadId);
             log.info("Transcoding finished. Uploading...");

             uploadDirectory(hlsOutputDir, hlsBucket, "movies/" + uploadId);

        } else if (purpose == UploadPurpose.MOVIE_POSTER || purpose == UploadPurpose.USER_AVATAR) {
             Path imgOutputDir = tempDir.resolve("images");
             Files.createDirectories(imgOutputDir);

             String format = com.google.common.io.Files.getFileExtension(input.toString());
             if(format.isEmpty()) format = "jpg";

             // Pass purpose to handle sizing logic
             imageProcessingService.processImageHierarchy(input, imgOutputDir.toString(), format, purpose);

             String prefix = (purpose == UploadPurpose.USER_AVATAR ? "users/avatars/" : "movies/posters/") + uploadId;
             uploadDirectory(imgOutputDir, publicBucket, prefix);
        }
        
        return videoDuration; // Return duration (null for images)
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
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }

        // Ensure a secure bucket exists for keys
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(secureBucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(secureBucket).build());
        }

        try (var stream = Files.walk(dir)) {
            stream.filter(Files::isRegularFile)
                    // IMPORTANT remove file patterns and temp files
                    .filter(path -> !path.getFileName().toString().contains("%"))
                    .filter(path -> !path.getFileName().toString().endsWith(".txt")) // Remove keyinfo.txt
                    .forEach(path -> {
                        try {
                            String relativePath = dir.relativize(path).toString().replace("\\", "/");
                            String objectName = prefix + "/" + relativePath;

                            String targetBucket = bucket;
                            if (path.toString().endsWith(".key")) {
                                targetBucket = secureBucket;
                            }

                            minioClient.putObject(
                                    PutObjectArgs.builder()
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
     * @param duration the duration of the video in seconds (null for non-video files or errors)
     */
    private void publishStatus(String uploadId, String status, String reason, Double duration) {
        try {
            MediaStatusUpdateEvent event = MediaStatusUpdateEvent.builder()
                    .uploadId(uploadId)
                    .status(status)
                    .reason(reason)
                    .duration(duration)
                    .build();
            String json = objectMapper.writeValueAsString(event);
            natsConnection.publish("media.status.update", json.getBytes(StandardCharsets.UTF_8));
            log.info("Published status update: {} for {} (duration: {}s)", status, uploadId, duration);
        } catch (Exception e) {
            log.error("Failed to publish status update", e);
        }
    }
}

