package com.bbmovie.transcodeworker.service.pipeline.stage;

import com.bbmovie.transcodeworker.service.nats.HeartbeatManager;
import com.bbmovie.transcodeworker.service.pipeline.dto.ExecuteTask;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeResult;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeTask;
import com.bbmovie.transcodeworker.service.pipeline.queue.PipelineQueues;
import com.bbmovie.transcodeworker.service.probe.FastProbeService;
import com.bbmovie.transcodeworker.service.scheduler.ResolutionCostCalculator;
import com.bbmovie.transcodeworker.service.scheduler.TranscodeScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stage 2: Prober
 * <p>
 * Responsibilities:
 * - Take ProbeTask from probeQueue
 * - Probe video metadata (via FastProbeService)
 * - Calculate resource cost
 * - Acquire scheduler resources (tryAcquire with timeout)
 * - Put ExecuteTask into executeQueue
 * <p>
 * Key design:
 * - Multiple prober threads for parallel probing
 * - Non-blocking resource acquisition (tryAcquire)
 * - Re-queues task if resources unavailable
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProberStage {

    @Value("${app.pipeline.prober-thread-count:5}")
    private int proberThreadCount;

    @Value("${app.pipeline.resource-acquire-timeout-seconds:10}")
    private int resourceAcquireTimeoutSeconds;

    @Value("${app.pipeline.probe-queue-poll-timeout-ms:500}")
    private int pollTimeoutMs;

    private final PipelineQueues pipelineQueues;
    private final FastProbeService fastProbeService;
    private final TranscodeScheduler scheduler;
    private final ResolutionCostCalculator costCalculator;
    private final HeartbeatManager heartbeatManager;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger activeProbers = new AtomicInteger(0);
    private ExecutorService proberExecutor;

    /**
     * Starts the prober stage with multiple worker threads.
     */
    public void start() {
        if (running.getAndSet(true)) {
            log.warn("ProberStage already running");
            return;
        }

        proberExecutor = Executors.newFixedThreadPool(proberThreadCount);

        for (int i = 0; i < proberThreadCount; i++) {
            final int proberIndex = i;
            proberExecutor.submit(() -> proberLoop(proberIndex));
        }

        log.debug("ProberStage started with {} threads", proberThreadCount);
    }

    /**
     * Prober worker loop.
     * Continuously takes tasks from the queue and probes them.
     */
    private void proberLoop(int proberIndex) {
        log.debug("Prober {} started", proberIndex);
        activeProbers.incrementAndGet();

        try {
            while (running.get()) {
                try {
                    // Poll with timeout (allows clean shutdown)
                    ProbeTask task = pipelineQueues.pollProbeTask(pollTimeoutMs, TimeUnit.MILLISECONDS);
                    if (task == null) {
                        continue; // Timeout, retry
                    }

                    processTask(task, proberIndex);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Error in prober {}", proberIndex, e);
                }
            }
        } finally {
            activeProbers.decrementAndGet();
            log.debug("Prober {} stopped", proberIndex);
        }
    }

    /**
     * Processes a single probe task.
     */
    private void processTask(ProbeTask task, int proberIndex) {
        String taskId = task.bucket() + "/" + task.key();
        log.debug("Prober {} processing: {}", proberIndex, taskId);

        HeartbeatManager.HeartbeatHandle heartbeat = null;

        try {
            // Start heartbeat for safety (probe should be fast, but just in case)
            heartbeat = heartbeatManager.startHeartbeat(task.natsMessage(), taskId);

            // Probe to get metadata and determine cost
            ProbeResult probeResult;
            int costWeight;

            if (task.isVideo()) {
                // Probe video for metadata
                probeResult = fastProbeService.probe(task.bucket(), task.key());
                costWeight = probeResult.peakCost();
                log.debug("Probed {}: {}x{}, peakCost={}", taskId,
                        probeResult.width(), probeResult.height(), costWeight);
            } else {
                // Images have minimal cost
                probeResult = ProbeResult.forImage(0, 0);
                costWeight = 1;
                log.debug("Image task {}: cost={}", taskId, costWeight);
            }

            // Try to acquire resources
            Optional<TranscodeScheduler.ResourceHandle> resourceHandle =
                    scheduler.tryAcquire(costWeight, Duration.ofSeconds(resourceAcquireTimeoutSeconds));

            if (resourceHandle.isEmpty()) {
                // Resources are not available, re-queue for later
                log.info("Resources unavailable for {} (cost={}), re-queuing", taskId, costWeight);

                // Stop heartbeat but don't ACK/NAK - we're re-queuing
                heartbeatManager.stopHeartbeat(heartbeat);
                heartbeat = null;

                // Re-queue the task (it will be picked up again)
                pipelineQueues.putProbeTask(task);
                return;
            }

            // Resources acquired! Create ExecuteTask
            ExecuteTask executeTask = ExecuteTask.from(task, probeResult, resourceHandle.get());

            // Stop heartbeat (will be restarted by ExecutorStage)
            heartbeatManager.stopHeartbeat(heartbeat);
            heartbeat = null;

            // Queue for execution
            pipelineQueues.putExecuteTask(executeTask);
            log.info("Queued execute task: {} (cost={}, queueTime={}ms)", taskId, costWeight, task.getQueueTimeMs());

        } catch (Exception e) {
            log.error("Failed to probe task: {}", taskId, e);

            // NAK the message for redelivery
            if (heartbeat != null) {
                heartbeatManager.stopAndNak(heartbeat);
            } else {
                task.natsMessage().nak();
            }
        }
    }

    /**
     * Stops the prober stage.
     */
    public void stop() {
        log.info("Stopping ProberStage");
        running.set(false);

        if (proberExecutor != null) {
            proberExecutor.shutdown();
            try {
                if (!proberExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    proberExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                proberExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Checks if the prober is running.
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Returns the number of active prober threads.
     */
    public int getActiveProberCount() {
        return activeProbers.get();
    }
}

