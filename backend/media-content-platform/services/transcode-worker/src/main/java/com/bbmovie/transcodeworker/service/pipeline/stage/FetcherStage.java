package com.bbmovie.transcodeworker.service.pipeline.stage;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.service.nats.NatsConnectionManager;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeTask;
import com.bbmovie.transcodeworker.service.pipeline.queue.PipelineQueues;
import com.bbmovie.transcodeworker.service.storage.MinioDownloadService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PullSubscribeOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Stage 1: Fetcher
 * <p>
 * Responsibilities:
 * - Pull messages from NATS JetStream
 * - Parse message payload (MinIO event)
 * - Extract bucket, key, purpose, uploadId
 * - Put ProbeTask into probeQueue
 * <p>
 * This stage is fast and simple - it just transforms NATS messages
 * into ProbeTask DTOs and queues them for probing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FetcherStage {

    @Value("${nats.consumer.fetch-batch-size:5}")
    private int fetchBatchSize;

    @Value("${nats.consumer.fetch-timeout-seconds:2}")
    private int fetchTimeoutSeconds;

    private final NatsConnectionManager natsConnectionManager;
    private final PipelineQueues pipelineQueues;
    private final MinioDownloadService minioDownloadService;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private JetStreamSubscription subscription;

    /**
     * Map of user metadata keys to UploadPurpose values.
     */
    private static final Map<String, UploadPurpose> PURPOSE_MAP = Map.of(
            "MOVIE_SOURCE", UploadPurpose.MOVIE_SOURCE,
            "MOVIE_TRAILER", UploadPurpose.MOVIE_TRAILER,
            "MOVIE_POSTER", UploadPurpose.MOVIE_POSTER,
            "USER_AVATAR", UploadPurpose.USER_AVATAR
    );

    /**
     * Starts the fetcher stage.
     * Creates a subscription and begins pulling messages.
     */
    public void start() throws Exception {
        if (running.getAndSet(true)) {
            log.warn("FetcherStage already running");
            return;
        }

        // Create a pull subscription
        PullSubscribeOptions options = PullSubscribeOptions.builder()
                .durable(natsConnectionManager.getConsumerDurable())
                .stream(natsConnectionManager.getStreamName())
                .build();

        subscription = natsConnectionManager.getJetStream()
                .subscribe(natsConnectionManager.getSubject(), options);

        log.debug("FetcherStage started - subject: {}, consumer: {}",
                natsConnectionManager.getSubject(), natsConnectionManager.getConsumerDurable());

        // Start fetch loop in virtual thread
        Thread.startVirtualThread(this::fetchLoop);
    }

    /**
     * Main fetch loop.
     * Continuously pulls messages and queues them for probing.
     */
    private void fetchLoop() {
        log.info("Fetcher loop started");

        while (running.get()) {
            try {
                // Fetch batch of messages
                List<Message> messages = subscription.fetch(
                        fetchBatchSize,
                        Duration.ofSeconds(fetchTimeoutSeconds)
                );

                if (messages.isEmpty()) {
                    continue;
                }

                log.debug("Fetched {} messages", messages.size());

                // Process each message
                for (Message message : messages) {
                    if (!running.get()) break;

                    try {
                        processMessage(message);
                    } catch (Exception e) {
                        // ACK to avoid infinite retry - most errors here are parse errors
                        // which won't be fixed by retrying
                        log.error("Failed to process message, ACK to skip: {}", e.getMessage());
                        message.ack();
                    }
                }

            } catch (Exception e) {
                log.error("Error in fetch loop", e);
                // Brief pause before retry
                // Virtual thread sleep unmounts automatically. No OS thread is blocked.
                try {
                    // SAFE with virtual thread, java 21+
                    // IT will Unmount the OS thread (Carrier Thread) immediately
                    // when timeout => the Virtual Thread will be remounted
                    // => no resource waste
                    //noinspection BusyWait
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("Fetcher loop stopped");
    }

    /**
     * Processes a single NATS message.
     * Parses the MinIO event and creates a ProbeTask.
     */
    private void processMessage(Message message) throws Exception {
        String payload = new String(message.getData(), StandardCharsets.UTF_8);
        log.trace("Processing message: {}", payload);

        JsonNode root = objectMapper.readTree(payload);
        JsonNode records = root.path("Records");

        if (records.isEmpty()) {
            log.warn("No records in message, skipping");
            message.ack();
            return;
        }

        // Process first record (MinIO sends one event per message typically)
        JsonNode record = records.get(0);
        String eventName = record.path("eventName").asText();

        // Only process object creation events
        if (!eventName.startsWith("s3:ObjectCreated:")) {
            log.debug("Ignoring non-creation event: {}", eventName);
            message.ack();
            return;
        }

        // Extract bucket and key
        JsonNode s3Node = record.path("s3");
        String bucket = s3Node.path("bucket").path("name").asText();
        String rawKey = s3Node.path("object").path("key").asText();
        
        // IMPORTANT: Decode URL-encoded key (e.g., %2F -> /)
        String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
        if (!rawKey.equals(key)) {
            log.debug("Decoded key: {} -> {}", rawKey, key);
        }

        // Get user metadata
        // MinIO sends metadata with different key formats depending on version
        // Try multiple variations: X-Amz-Meta-*, x-amz-meta-*, lowercase, etc.
        JsonNode userMetadata = s3Node.path("object").path("userMetadata");
        
        // DEBUG: Log all metadata keys to help troubleshoot
        if (log.isDebugEnabled()) {
            log.debug("User metadata keys: {}", userMetadata.fieldNames().hasNext() 
                    ? userMetadata.toString()
                    : "EMPTY");
        }
        
        String purposeStr = extractMetadataValue(userMetadata, 
                "X-Amz-Meta-Purpose", "x-amz-meta-purpose", "purpose", "Purpose");
        
        // Try both upload-id and video-id (frontend may use video-id)
        String uploadId = extractMetadataValue(userMetadata, 
                "X-Amz-Meta-Upload-Id", "x-amz-meta-upload-id", "upload-id", "uploadId",
                "X-Amz-Meta-Video-Id", "x-amz-meta-video-id", "video-id", "videoId");

        if (purposeStr == null || uploadId == null) {
            log.warn("Missing required metadata for {}/{} - purpose={}, uploadId={}, metadata={}", 
                    bucket, key, purposeStr, uploadId, userMetadata);
            message.ack();
            return;
        }

        // Map purpose string to enum
        UploadPurpose purpose = PURPOSE_MAP.get(purposeStr.toUpperCase());
        if (purpose == null) {
            log.warn("Unknown purpose: {} for {}/{}, skipping", purposeStr, bucket, key);
            message.ack();
            return;
        }

        // Get file info from MinIO
        long fileSize;
        String contentType;
        try {
            fileSize = minioDownloadService.getObjectSize(bucket, key);
            contentType = minioDownloadService.getContentType(bucket, key);
        } catch (Exception e) {
            // File isn't found or MinIO error - ACK to avoid infinite retry
            // These errors are not recoverable by retrying
            log.error("Failed to get file info for {}/{} - ACK to skip: {}", bucket, key, e.getMessage());
            message.ack();
            return;
        }

        // Create a ProbeTask
        ProbeTask task = ProbeTask.create(
                message,
                bucket,
                key,
                purpose,
                uploadId,
                contentType,
                fileSize
        );

        // Queue for probing (blocks if the queue is full)
        pipelineQueues.putProbeTask(task);
        log.info("Queued probe task: {}/{} (purpose: {}, size: {} bytes)",
                bucket, key, purpose, fileSize);
    }

    /**
     * Extracts metadata value with fallback keys.
     */
    private String extractMetadataValue(JsonNode metadata, String... keys) {
        for (String key : keys) {
            JsonNode value = metadata.path(key);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    /**
     * Stops the fetcher stage.
     */
    public void stop() {
        log.info("Stopping FetcherStage");
        running.set(false);

        if (subscription != null) {
            try {
                subscription.unsubscribe();
            } catch (Exception e) {
                log.warn("Error unsubscribing: {}", e.getMessage());
            }
        }
    }

    /**
     * Checks if the fetcher is running.
     */
    public boolean isRunning() {
        return running.get();
    }
}

