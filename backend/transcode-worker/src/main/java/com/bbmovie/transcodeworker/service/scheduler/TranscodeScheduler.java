package com.bbmovie.transcodeworker.service.scheduler;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler that manages CPU/resource capacity for video transcoding operations.
 * Uses weighted semaphore to prevent server overload by limiting concurrent transcoding jobs
 * based on resolution complexity.
 * <p>
 * Auto-detects available CPU cores and reserves some for OS/DB operations.
 * Scales automatically from high-end servers (16+ cores) to budget VPS (2-4 cores).
 */
@Slf4j
@Component
public class TranscodeScheduler {

    private final Semaphore semaphore;
    private final AtomicInteger currentUsage = new AtomicInteger(0);
    /**
     * Gets the maximum capacity (total logical processors available for transcoding).
     */
    @Getter
    private final int maxCapacity;
    
    /**
     * Total logical processors detected on the system.
     */
    @Getter
    private final int totalLogicalProcessors;

    /**
     * Creates a TranscodeScheduler with auto-detected capacity.
     * <p>
     * Auto-detection logic:
     * - Detects total logical processors (Runtime.getRuntime().availableProcessors())
     * - For systems with > 4 cores: Reserves 2 cores for OS/DB
     * - For systems with <= 4 cores: Uses all cores (budget VPS scenario)
     * 
     * @param maxCapacityOverride Optional override for max capacity (if not provided, auto-detects)
     */
    public TranscodeScheduler(
            @Value("${app.transcode.max-capacity:0}") int maxCapacityOverride) {
        
        // Detect total logical processors
        this.totalLogicalProcessors = Runtime.getRuntime().availableProcessors();
        
        // Use override if provided, otherwise auto-calculate
        if (maxCapacityOverride > 0) {
            this.maxCapacity = maxCapacityOverride;
            log.info("Using configured max capacity: {} slots", maxCapacity);
        } else {
            // Auto-detect: Reserve cores for OS/DB on systems with > 4 cores
            if (totalLogicalProcessors > 4) {
                this.maxCapacity = totalLogicalProcessors - 2; // Reserve 2 cores
            } else {
                this.maxCapacity = totalLogicalProcessors; // Use all cores on budget VPS
            }
            log.info("Auto-detected {} logical processors. Scheduler capacity: {} slots (reserved {} for OS/DB)", 
                    totalLogicalProcessors, maxCapacity, totalLogicalProcessors > 4 ? 2 : 0);
        }
        
        this.semaphore = new Semaphore(this.maxCapacity, true); // Fair semaphore
        
        log.info("TranscodeScheduler initialized - Total cores: {}, Capacity: {} slots", 
                totalLogicalProcessors, maxCapacity);
    }

    /**
     * Acquires permits for a transcoding job based on resolution cost.
     * <p>
     * Important behavior:
     * - If costWeight > maxCapacity: The job will WAIT until maxCapacity slots are available
     *   (This ensures high-cost jobs don't overload the system)
     * - If costWeight <= maxCapacity: The job acquires exactly costWeight slots
     * <p>
     * The actual threads allocated to FFmpeg will be clamped to maxCapacity:
     * - threads = min(costWeight, maxCapacity)
     * 
     * @param costWeight The cost weight of the resolution (144p=1, 240p=2, 360p=4, 480p=8, 720p=16, 1080p=32)
     * @return ResourceHandle containing both costWeight and actual threads to use
     * @throws InterruptedException if interrupted while waiting
     */
    public ResourceHandle acquire(int costWeight) throws InterruptedException {
        // Calculate actual threads: clamp to maxCapacity
        int actualThreads = Math.min(costWeight, maxCapacity);
        
        // If cost exceeds maxCapacity, we need to wait for maxCapacity slots to be available
        // This ensures the system never overloads

        if (costWeight > maxCapacity) {
            log.info("Cost weight {} exceeds max capacity {}. Will wait for {} slots and use {} threads for FFmpeg", 
                    costWeight, maxCapacity, actualThreads, actualThreads);
        } else {
            log.debug("Acquiring {} resource slots (current usage: {}/{})",
                    actualThreads, currentUsage.get(), maxCapacity);
        }
        
        // Acquire permits (blocks if not enough available)
        // This will wait until enough slots are free
        semaphore.acquire(actualThreads);
        
        int newUsage = currentUsage.addAndGet(actualThreads);
        log.info("Acquired {} slots (cost: {}, threads: {}) - Total usage: {}/{}, {}%",
                actualThreads, costWeight, actualThreads, newUsage, maxCapacity,
                String.format("%.1f", newUsage * 100.0 / maxCapacity));
        
        // Store both costWeight and actualThreads in the handle
        return new ResourceHandle(costWeight, actualThreads);
    }

    /**
     * Releases resources back to the scheduler.
     * This should always be called in a final block.
     * <p>
     * Releases the actual slots that were acquired (not the cost weight).
     * If cost > maxCapacity, only maxCapacity slots were acquired, so only those are released.
     * 
     * @param handle The resource handles to release
     */
    public void release(ResourceHandle handle) {
        if (handle == null || handle.isReleased()) {
            return;
        }
        
        // Release the actual slots that were acquired (clamped to maxCapacity)
        int slotsToRelease = handle.getActualThreads();
        int costWeight = handle.getCostWeight();
        
        semaphore.release(slotsToRelease);
        
        int newUsage = currentUsage.addAndGet(-slotsToRelease);
        log.info("Released {} slots (cost: {}, threads: {}) - Total usage: {}/{}, {}%",
                slotsToRelease, costWeight, slotsToRelease, newUsage, maxCapacity,
                String.format("%.1f", newUsage * 100.0 / maxCapacity));
        
        handle.markReleased();
    }

    /**
     * Resource handle that tracks acquired resources.
     * Must be released in a final block to prevent resource leaks.
     */
    public static class ResourceHandle {

        @Getter
        private final int costWeight;      // Original cost weight (for logging)

        @Getter
        private final int actualThreads;   // Actual threads to use for FFmpeg (clamped to maxCapacity)

        private volatile boolean released = false;

        private ResourceHandle(int costWeight, int actualThreads) {
            this.costWeight = costWeight;
            this.actualThreads = actualThreads;
        }

        private boolean isReleased() {
            return released;
        }

        private void markReleased() {
            this.released = true;
        }
    }
}

