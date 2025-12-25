package com.bbmovie.transcodeworker.service.pipeline;

import com.bbmovie.transcodeworker.service.nats.HeartbeatManager;
import com.bbmovie.transcodeworker.service.nats.NatsConnectionManager;
import com.bbmovie.transcodeworker.service.pipeline.queue.PipelineQueues;
import com.bbmovie.transcodeworker.service.pipeline.stage.ExecutorStage;
import com.bbmovie.transcodeworker.service.pipeline.stage.FetcherStage;
import com.bbmovie.transcodeworker.service.pipeline.stage.ProberStage;
import com.bbmovie.transcodeworker.service.scheduler.TranscodeScheduler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates the 3-Stage Pipeline for media processing.
 * <p>
 * Pipeline structure:
 * <pre>
 * NATS → [FetcherStage] → probeQueue → [ProberStage] → executeQueue → [ExecutorStage] → ACK
 * </pre>
 * <p>
 * Responsibilities:
 * - Initialize NATS connection
 * - Start all stages in correct order
 * - Graceful shutdown
 * - Monitoring/health checks
 * <p>
 * Enable with: app.pipeline.enabled=true
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PipelineOrchestrator {

    private final NatsConnectionManager natsConnectionManager;
    private final TranscodeScheduler scheduler;
    private final PipelineQueues pipelineQueues;
    private final FetcherStage fetcherStage;
    private final ProberStage proberStage;
    private final ExecutorStage executorStage;
    private final HeartbeatManager heartbeatManager;

    @Value("${app.pipeline.monitor-interval-seconds:30}")
    private int monitorIntervalSeconds;

    private final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Initializes and starts the pipeline.
     */
    @PostConstruct
    public void init() {
        try {
            // 1. Initialize NATS
            natsConnectionManager.initialize();
            natsConnectionManager.ensureStreamExists();

            // 2. Setup consumer with max_ack_pending = maxCapacity
            natsConnectionManager.setupConsumer(scheduler.getMaxCapacity());

            // 3. Start stages in reverse order (consumers first)
            executorStage.start();
            log.info("ExecutorStage started");

            proberStage.start();
            log.info("ProberStage started");

            fetcherStage.start();
            log.info("FetcherStage started");

            // 4. Start monitoring
            startMonitoring();

            log.info("Scheduler capacity: {} slots", scheduler.getMaxCapacity());
            log.info("Probe queue capacity: {}", pipelineQueues.getProbeQueueRemainingCapacity());
            log.info("Execute queue capacity: {}", pipelineQueues.getExecuteQueueRemainingCapacity());

        } catch (Exception e) {
            log.error("Failed to start pipeline", e);
            throw new RuntimeException("Pipeline startup failed", e);
        }
    }

    /**
     * Starts periodic monitoring of pipeline health.
     */
    private void startMonitoring() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                logStatus();
            } catch (Exception e) {
                log.error("Error in monitor", e);
            }
        }, monitorIntervalSeconds, monitorIntervalSeconds, TimeUnit.SECONDS);
    }

    /**
     * Logs current pipeline status.
     */
    public void logStatus() {
        log.info("Pipeline Status: " +
                        "fetcher={}, " +
                        "probers={}/{}, " +
                        "executors={}, " +
                        "probeQueue={}, " +
                        "executeQueue={}, " +
                        "scheduler={}/{} ({}%), " +
                        "heartbeats={}",
                fetcherStage.isRunning() ? "running" : "stopped",
                proberStage.getActiveProberCount(), proberStage.isRunning() ? "active" : "stopped",
                executorStage.getActiveExecutorCount(),
                pipelineQueues.getProbeQueueSize(),
                pipelineQueues.getExecuteQueueSize(),
                scheduler.getCurrentUsage(),
                scheduler.getMaxCapacity(),
                String.format("%.1f", scheduler.getUsagePercentage()),
                heartbeatManager.getActiveHeartbeatCount()
        );
    }

    /**
     * Graceful shutdown of the pipeline.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting Down Pipeline");

        // Stop fetcher first (stop receiving new work)
        fetcherStage.stop();
        log.info("FetcherStage stopped");

        // Stop prober (let queued items drain)
        proberStage.stop();
        log.info("ProberStage stopped");

        // Stop executor last (finish in-progress work)
        executorStage.stop();
        log.info("ExecutorStage stopped");

        // Stop heartbeat manager
        heartbeatManager.shutdown();
        log.info("HeartbeatManager stopped");

        // Stop monitor
        monitorExecutor.shutdown();
        try {
            boolean terminated = monitorExecutor.awaitTermination(5, TimeUnit.SECONDS);
            if (!terminated) {
                log.warn("Timeout waiting for monitor executor");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Clear queues
        pipelineQueues.clear();

        log.info("Pipeline Shutdown Complete");
    }

    /**
     * Checks if the pipeline is healthy.
     */
    public boolean isHealthy() {
        return fetcherStage.isRunning() &&
                proberStage.isRunning() &&
                executorStage.isRunning() &&
                natsConnectionManager.isConnected();
    }

    /**
     * Returns pipeline statistics.
     */
    public PipelineStats getStats() {
        return new PipelineStats(
                fetcherStage.isRunning(),
                proberStage.isRunning(),
                proberStage.getActiveProberCount(),
                executorStage.isRunning(),
                executorStage.getActiveExecutorCount(),
                pipelineQueues.getProbeQueueSize(),
                pipelineQueues.getExecuteQueueSize(),
                scheduler.getCurrentUsage(),
                scheduler.getMaxCapacity(),
                heartbeatManager.getActiveHeartbeatCount()
        );
    }

    /**
     * Pipeline statistics record.
     */
    public record PipelineStats(
            boolean fetcherRunning,
            boolean proberRunning,
            int activeProbers,
            boolean executorRunning,
            int activeExecutors,
            int probeQueueSize,
            int executeQueueSize,
            int schedulerUsage,
            int schedulerCapacity,
            int activeHeartbeats
    ) {
        public double getSchedulerUsagePercent() {
            return schedulerCapacity > 0 ? schedulerUsage * 100.0 / schedulerCapacity : 0;
        }
    }
}

