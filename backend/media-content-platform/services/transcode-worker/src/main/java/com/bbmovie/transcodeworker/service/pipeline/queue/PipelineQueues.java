package com.bbmovie.transcodeworker.service.pipeline.queue;

import com.bbmovie.transcodeworker.service.pipeline.dto.ExecuteTask;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeTask;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manages the blocking queues for the 3-stage pipeline.
 * <p>
 * Queue structure:
 * <pre>
 * FetcherStage → [probeQueue] → ProberStage → [executeQueue] → ExecutorStage
 * </pre>
 * <p>
 * Both queues are bounded to prevent memory issues when downstream
 * stages are slower than upstream stages.
 */
@Slf4j
@Component
public class PipelineQueues {

    /**
     * Queue capacity for probe tasks (Fetcher → Prober).
     * Higher capacity allows fetcher to prefetch more tasks.
     */
    @Value("${app.pipeline.probe-queue-capacity:100}")
    private int probeQueueCapacity;

    /**
     * Queue capacity for execute tasks (Prober → Executor).
     * Lower capacity because tasks here have already acquired resources.
     */
    @Value("${app.pipeline.execute-queue-capacity:50}")
    private int executeQueueCapacity;

    /**
     * Queue for tasks awaiting probing.
     * Flow: FetcherStage → ProberStage
     */
    @Getter
    private BlockingQueue<ProbeTask> probeQueue;

    /**
     * Queue for tasks awaiting execution.
     * Flow: ProberStage → ExecutorStage
     */
    @Getter
    private BlockingQueue<ExecuteTask> executeQueue;

    @PostConstruct
    public void init() {
        this.probeQueue = new LinkedBlockingQueue<>(probeQueueCapacity);
        this.executeQueue = new LinkedBlockingQueue<>(executeQueueCapacity);

        log.info("Pipeline queues initialized - probeQueue capacity: {}, executeQueue capacity: {}", probeQueueCapacity, executeQueueCapacity);
    }

    // ==================== Probe Queue Operations ====================

    /**
     * Adds a task to the probe queue, blocking if full.
     *
     * @param task Task to add
     * @throws InterruptedException if interrupted while waiting
     */
    public void putProbeTask(ProbeTask task) throws InterruptedException {
        probeQueue.put(task);
        log.debug("Added probe task for {}/{} - queue size: {}",
                task.bucket(), task.key(), probeQueue.size());
    }

    /**
     * Adds a task to the probe queue with timeout.
     *
     * @param task    Task to add
     * @param timeout Timeout duration
     * @param unit    Timeout unit
     * @return true if added, false if timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean offerProbeTask(ProbeTask task, long timeout, TimeUnit unit) throws InterruptedException {
        boolean success = probeQueue.offer(task, timeout, unit);
        if (success) {
            log.debug("Offered probe task for {}/{} - queue size: {}",
                    task.bucket(), task.key(), probeQueue.size());
        } else {
            log.warn("Failed to offer probe task for {}/{} - queue full",
                    task.bucket(), task.key());
        }
        return success;
    }

    /**
     * Takes a task from the probe queue, blocking if empty.
     *
     * @return ProbeTask
     * @throws InterruptedException if interrupted while waiting
     */
    public ProbeTask takeProbeTask() throws InterruptedException {
        return probeQueue.take();
    }

    /**
     * Polls a task from the probe queue with timeout.
     *
     * @param timeout Timeout duration
     * @param unit    Timeout unit
     * @return ProbeTask or null if timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public ProbeTask pollProbeTask(long timeout, TimeUnit unit) throws InterruptedException {
        return probeQueue.poll(timeout, unit);
    }

    // ==================== Execute Queue Operations ====================

    /**
     * Adds a task to the execute queue, blocking if full.
     *
     * @param task Task to add
     * @throws InterruptedException if interrupted while waiting
     */
    public void putExecuteTask(ExecuteTask task) throws InterruptedException {
        executeQueue.put(task);
        log.debug("Added execute task for {}/{} - queue size: {}",
                task.bucket(), task.key(), executeQueue.size());
    }

    /**
     * Adds a task to the execute queue with timeout.
     *
     * @param task    Task to add
     * @param timeout Timeout duration
     * @param unit    Timeout unit
     * @return true if added, false if timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean offerExecuteTask(ExecuteTask task, long timeout, TimeUnit unit) throws InterruptedException {
        boolean success = executeQueue.offer(task, timeout, unit);
        if (success) {
            log.debug("Offered execute task for {}/{} - queue size: {}",
                    task.bucket(), task.key(), executeQueue.size());
        } else {
            log.warn("Failed to offer execute task for {}/{} - queue full",
                    task.bucket(), task.key());
        }
        return success;
    }

    /**
     * Takes a task from the execute queue, blocking if empty.
     *
     * @return ExecuteTask
     * @throws InterruptedException if interrupted while waiting
     */
    public ExecuteTask takeExecuteTask() throws InterruptedException {
        return executeQueue.take();
    }

    /**
     * Polls a task from the execute queue with timeout.
     *
     * @param timeout Timeout duration
     * @param unit    Timeout unit
     * @return ExecuteTask or null if timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public ExecuteTask pollExecuteTask(long timeout, TimeUnit unit) throws InterruptedException {
        return executeQueue.poll(timeout, unit);
    }

    // ==================== Monitoring ====================

    /**
     * Returns current size of probe queue.
     */
    public int getProbeQueueSize() {
        return probeQueue.size();
    }

    /**
     * Returns current size of execute queue.
     */
    public int getExecuteQueueSize() {
        return executeQueue.size();
    }

    /**
     * Returns remaining capacity in probe queue.
     */
    public int getProbeQueueRemainingCapacity() {
        return probeQueue.remainingCapacity();
    }

    /**
     * Returns remaining capacity in execute queue.
     */
    public int getExecuteQueueRemainingCapacity() {
        return executeQueue.remainingCapacity();
    }

    /**
     * Checks if probe queue is empty.
     */
    public boolean isProbeQueueEmpty() {
        return probeQueue.isEmpty();
    }

    /**
     * Checks if execute queue is empty.
     */
    public boolean isExecuteQueueEmpty() {
        return executeQueue.isEmpty();
    }

    /**
     * Clears both queues (for shutdown/reset).
     */
    public void clear() {
        probeQueue.clear();
        executeQueue.clear();
        log.info("Pipeline queues cleared");
    }

    /**
     * Logs current queue status.
     */
    public void logStatus() {
        log.info("Pipeline queues status - probeQueue: {}/{}, executeQueue: {}/{}",
                probeQueue.size(), probeQueueCapacity,
                executeQueue.size(), executeQueueCapacity);
    }
}

