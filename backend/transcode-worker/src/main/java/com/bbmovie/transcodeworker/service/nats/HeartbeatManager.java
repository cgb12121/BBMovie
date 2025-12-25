package com.bbmovie.transcodeworker.service.nats;

import io.nats.client.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages NATS JetStream heartbeats to prevent message timeout during long-running tasks.
 * <p>
 * For video transcoding, processing can take minutes to hours. Without heartbeats,
 * NATS would redeliver the message thinking the consumer failed.
 * <p>
 * This manager:
 * - Starts periodic inProgress() calls when processing begins
 * - Stops heartbeat when processing completes (ACK/NAK)
 * - Handles cleanup on shutdown
 */
@Slf4j
@Component
public class HeartbeatManager {

    @Value("${nats.consumer.heartbeat-interval-seconds:30}")
    private int heartbeatIntervalSeconds;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    /**
     * Tracks active heartbeats by message subject ID.
     */
    private final Map<String, ScheduledFuture<?>> activeHeartbeats = new ConcurrentHashMap<>();

    /**
     * Starts heartbeat for a NATS message.
     * Periodically calls inProgress() to prevent timeout.
     *
     * @param message NATS message to keep alive
     * @param taskId  Unique task identifier for logging
     * @return Heartbeat handle for stopping
     */
    public HeartbeatHandle startHeartbeat(Message message, String taskId) {
        String heartbeatId = taskId + "_" + System.nanoTime();

        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                message.inProgress();
                log.debug("Heartbeat sent for task: {}", taskId);
            } catch (Exception e) {
                log.warn("Failed to send heartbeat for task: {}", taskId, e);
            }
        }, heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);

        activeHeartbeats.put(heartbeatId, future);
        log.debug("Started heartbeat for task: {} (interval: {}s)", taskId, heartbeatIntervalSeconds);

        return new HeartbeatHandle(heartbeatId, message);
    }

    /**
     * Stops a heartbeat.
     * Should be called when processing completes (success or failure).
     *
     * @param handle Heartbeat handle returned from startHeartbeat
     */
    public void stopHeartbeat(HeartbeatHandle handle) {
        if (handle == null) return;

        ScheduledFuture<?> future = activeHeartbeats.remove(handle.heartbeatId());
        if (future != null) {
            future.cancel(false);
            log.debug("Stopped heartbeat: {}", handle.heartbeatId());
        }
    }

    /**
     * Stops heartbeat and acknowledges the message.
     * Convenience method for successful completion.
     *
     * @param handle Heartbeat handle
     */
    public void stopAndAck(HeartbeatHandle handle) {
        stopHeartbeat(handle);
        if (handle != null && handle.message() != null) {
            handle.message().ack();
            log.debug("ACK sent for heartbeat: {}", handle.heartbeatId());
        }
    }

    /**
     * Stops heartbeat and negative acknowledges the message.
     * Convenience method for failed processing (triggers redelivery).
     *
     * @param handle Heartbeat handle
     */
    public void stopAndNak(HeartbeatHandle handle) {
        stopHeartbeat(handle);
        if (handle != null && handle.message() != null) {
            handle.message().nak();
            log.debug("NAK sent for heartbeat: {}", handle.heartbeatId());
        }
    }

    /**
     * Returns the number of active heartbeats.
     * Useful for monitoring.
     */
    public int getActiveHeartbeatCount() {
        return activeHeartbeats.size();
    }

    /**
     * Shuts down the heartbeat scheduler.
     * Should be called during application shutdown.
     */
    public void shutdown() {
        log.info("Shutting down HeartbeatManager ({} active heartbeats)", activeHeartbeats.size());

        // Cancel all active heartbeats
        activeHeartbeats.values().forEach(f -> f.cancel(false));
        activeHeartbeats.clear();

        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("HeartbeatManager shutdown complete");
    }

    /**
     * Handle for managing a single heartbeat.
     */
    public record HeartbeatHandle(String heartbeatId, Message message) {
    }
}

