package com.bbmovie.transcodeworker.service.pipeline.stage;

import com.bbmovie.transcodeworker.service.nats.HeartbeatManager;
import com.bbmovie.transcodeworker.service.nats.StatusPublisher;
import com.bbmovie.transcodeworker.service.pipeline.dto.ExecuteTask;
import com.bbmovie.transcodeworker.service.pipeline.queue.PipelineQueues;
import com.bbmovie.transcodeworker.service.processing.MediaProcessor;
import com.bbmovie.transcodeworker.service.processing.MediaProcessorFactory;
import com.bbmovie.transcodeworker.service.scheduler.TranscodeScheduler;
import com.bbmovie.transcodeworker.service.storage.MinioDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stage 3: Executor
 * <p>
 * Responsibilities:
 * - Take ExecuteTask from executeQueue
 * - Download file from MinIO
 * - Delegate to appropriate MediaProcessor
 * - Publish status updates
 * - Release scheduler resources
 * - ACK/NAK NATS message
 * <p>
 * Refactored to use Strategy pattern - delegates actual processing
 * to MediaProcessor implementations (VideoProcessor, ImageProcessor).
 * <p>
 * Dependencies reduced from 10 to 7:
 * - PipelineQueues (queue operations)
 * - MinioDownloadService (file download)
 * - MediaProcessorFactory (processor selection)
 * - TranscodeScheduler (resource release)
 * - HeartbeatManager (NATS heartbeat)
 * - StatusPublisher (status updates)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExecutorStage {

    // Core pipeline dependencies
    private final PipelineQueues pipelineQueues;
    private final TranscodeScheduler scheduler;

    // NATS communication
    private final HeartbeatManager heartbeatManager;
    private final StatusPublisher statusPublisher;

    // Processing
    private final MinioDownloadService downloadService;
    private final MediaProcessorFactory processorFactory;

    @Value("${app.transcode.temp-dir}")
    private String baseTempDir;

    @Value("${app.pipeline.executor-thread-count:4}")
    private int executorThreadCount;

    @Value("${app.pipeline.execute-queue-poll-timeout-ms:500}")
    private int pollTimeoutMs;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger activeExecutors = new AtomicInteger(0);
    private ExecutorService executorService;

    /**
     * Starts the executor stage.
     */
    public void start() {
        if (running.getAndSet(true)) {
            log.warn("ExecutorStage already running");
            return;
        }

        // Use virtual threads for I/O-heavy operations
        executorService = Executors.newVirtualThreadPerTaskExecutor();

        // Start worker threads
        for (int i = 0; i < executorThreadCount; i++) {
            final int executorIndex = i;
            executorService.submit(() -> executorLoop(executorIndex));
        }

        log.info("ExecutorStage started with {} threads", executorThreadCount);
    }

    /**
     * Executor worker loop.
     */
    private void executorLoop(int executorIndex) {
        log.debug("Executor {} started", executorIndex);
        activeExecutors.incrementAndGet();

        try {
            while (running.get()) {
                try {
                    ExecuteTask task = pipelineQueues.pollExecuteTask(pollTimeoutMs, TimeUnit.MILLISECONDS);
                    if (task == null) {
                        continue;
                    }

                    processTask(task, executorIndex);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error in executor {}", executorIndex, e);
                }
            }
        } finally {
            activeExecutors.decrementAndGet();
            log.debug("Executor {} stopped", executorIndex);
        }
    }

    /**
     * Processes a single execute task.
     */
    private void processTask(ExecuteTask task, int executorIndex) {
        String taskId = task.bucket() + "/" + task.key();
        log.info("Executor {} processing: {} (cost={}, threads={})",
                executorIndex, taskId,
                task.resourceHandle().getCostWeight(),
                task.resourceHandle().getActualThreads());

        HeartbeatManager.HeartbeatHandle heartbeat = null;
        Path tempDir = null;

        try {
            // Start heartbeat for long-running processing
            heartbeat = heartbeatManager.startHeartbeat(task.natsMessage(), taskId);

            // Publish processing status
            statusPublisher.publishProcessing(task.uploadId());

            // Create a temp directory
            tempDir = createTempDir(task.uploadId());

            // Download file
            Path inputFile = tempDir.resolve("input" + getExtension(task.key()));
            downloadService.downloadToFile(task.bucket(), task.key(), inputFile);
            log.debug("Downloaded {} to {}", taskId, inputFile);

            // Get processor and process
            MediaProcessor processor = processorFactory.getProcessor(task.purpose());
            Path outputDir = tempDir.resolve("output");
            Files.createDirectories(outputDir);

            MediaProcessor.ProcessingResult result = processor.process(task, inputFile, outputDir);

            if (result.success()) {
                // Success! ACK the message FIRST before any other operations
                heartbeatManager.stopAndAck(heartbeat);
                heartbeat = null;  // Prevent double ACK/NAK
                log.info("✅ ACK sent - Completed task: {} (duration: {}s)", taskId, result.duration());

                // Status publishing is best-effort, don't fail on this
                try {
                    statusPublisher.publishCompleted(task.uploadId(), result.duration());
                } catch (Exception statusEx) {
                    log.warn("Failed to publish completed status for {}: {}", taskId, statusEx.getMessage());
                }
            } else {
                throw new RuntimeException(result.errorMessage());
            }

        } catch (Exception e) {
            log.error("Failed to execute task: {}", taskId, e);

            // NAK for redelivery - ONLY if we haven't already ACKed
            if (heartbeat != null) {
                heartbeatManager.stopAndNak(heartbeat);
                log.info("❌ NAK sent - Task will be redelivered: {}", taskId);
            }
            // If heartbeat is null, message was already ACKed - don't NAK!

            try {
                statusPublisher.publishFailed(task.uploadId(), e.getMessage());
            } catch (Exception statusEx) {
                log.warn("Failed to publish failed status for {}: {}", taskId, statusEx.getMessage());
            }

        } finally {
            // ALWAYS release resources
            scheduler.release(task.resourceHandle());

            // Cleanup temp directory
            if (tempDir != null) {
                cleanupTempDir(tempDir);
            }
        }
    }

    /**
     * Creates a temporary directory for processing.
     */
    private Path createTempDir(String uploadId) throws Exception {
        Path tempDir = Paths.get(baseTempDir, uploadId + "_" + UUID.randomUUID());
        Files.createDirectories(tempDir);
        return tempDir;
    }

    /**
     * Cleans up the temporary directory.
     */
    private void cleanupTempDir(Path tempDir) {
        try {
            FileUtils.deleteDirectory(tempDir.toFile());
            log.debug("Cleaned up temp directory: {}", tempDir);
        } catch (Exception e) {
            log.warn("Failed to cleanup temp directory: {}", tempDir, e);
        }
    }

    /**
     * Extracts file extension from key.
     */
    private String getExtension(String key) {
        int lastDot = key.lastIndexOf('.');
        return lastDot > 0 ? key.substring(lastDot) : "";
    }

    /**
     * Stops the executor stage.
     */
    public void stop() {
        log.info("Stopping ExecutorStage");
        running.set(false);

        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("Executor did not terminate gracefully, forcing shutdown");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Checks if the executor is running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns the number of active executor threads.
     */
    public int getActiveExecutorCount() {
        return activeExecutors.get();
    }
}
