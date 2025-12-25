package com.bbmovie.transcodeworker.service.pipeline;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeResult;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeTask;
import com.bbmovie.transcodeworker.service.pipeline.queue.PipelineQueues;
import com.bbmovie.transcodeworker.service.scheduler.TranscodeScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the 3-Stage Pipeline.
 * <p>
 * These tests verify the pipeline stages work together correctly.
 * Requires running infrastructure (NATS, MinIO) or use Testcontainers.
 * <p>
 * Run with: mvn test -Dtest=PipelineIntegrationTest -Dspring.profiles.active=test
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Pipeline Integration Tests")
@Disabled("Enable when test infrastructure is available")
class PipelineIntegrationTest {

    @Autowired
    private PipelineQueues pipelineQueues;

    @Autowired
    private TranscodeScheduler scheduler;

    @BeforeEach
    void setUp() {
        pipelineQueues.clear();
    }

    @Test
    @DisplayName("Pipeline should process tasks through all stages")
    void shouldProcessTasksThroughAllStages() throws Exception {
        // This test requires full infrastructure
        // See TESTING.md for manual testing instructions
    }

    @Test
    @DisplayName("Scheduler should handle concurrent resource requests")
    void shouldHandleConcurrentResourceRequests() throws InterruptedException {
        int numTasks = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numTasks);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);
        AtomicInteger currentConcurrent = new AtomicInteger(0);

        for (int i = 0; i < numTasks; i++) {
            final int taskNum = i;
            int cost = (taskNum % 4) + 1; // Costs 1-4

            executor.submit(() -> {
                try {
                    var handle = scheduler.acquire(cost);
                    int concurrent = currentConcurrent.incrementAndGet();
                    maxConcurrent.updateAndGet(max -> Math.max(max, concurrent));

                    Thread.sleep(10); // Simulate work

                    currentConcurrent.decrementAndGet();
                    scheduler.release(handle);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Log error
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(numTasks);
        assertThat(maxConcurrent.get()).isLessThanOrEqualTo(scheduler.getMaxCapacity());
    }

    @Test
    @DisplayName("Queue should handle producer-consumer pattern")
    void shouldHandleProducerConsumerPattern() throws Exception {
        int numMessages = 100;
        CountDownLatch producerDone = new CountDownLatch(1);
        CountDownLatch consumerDone = new CountDownLatch(numMessages);
        AtomicInteger consumed = new AtomicInteger(0);

        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < numMessages; i++) {
                    ProbeTask task = ProbeTask.create(
                            null, "bucket", "video" + i + ".mp4",
                            UploadPurpose.MOVIE_SOURCE, "upload-" + i,
                            "video/mp4", 1024L
                    );
                    pipelineQueues.putProbeTask(task);
                }
                producerDone.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer threads
        ExecutorService consumers = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            consumers.submit(() -> {
                while (consumed.get() < numMessages) {
                    try {
                        ProbeTask task = pipelineQueues.pollProbeTask(100, TimeUnit.MILLISECONDS);
                        if (task != null) {
                            consumed.incrementAndGet();
                            consumerDone.countDown();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }

        producer.start();
        
        // Wait for completion
        boolean allConsumed = consumerDone.await(10, TimeUnit.SECONDS);
        consumers.shutdownNow();

        assertThat(allConsumed).isTrue();
        assertThat(consumed.get()).isEqualTo(numMessages);
    }

    @Test
    @DisplayName("tryAcquire should respect timeout")
    void tryAcquireShouldRespectTimeout() throws Exception {
        // Fill capacity
        int capacity = scheduler.getMaxCapacity();
        var handles = new java.util.ArrayList<TranscodeScheduler.ResourceHandle>();
        
        for (int i = 0; i < capacity; i++) {
            handles.add(scheduler.acquire(1));
        }

        // Try to acquire with timeout
        long start = System.currentTimeMillis();
        var result = scheduler.tryAcquire(1, Duration.ofMillis(200));
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result).isEmpty();
        assertThat(elapsed).isBetween(200L, 400L);

        // Release one slot
        scheduler.release(handles.get(0));

        // Now should succeed
        result = scheduler.tryAcquire(1, Duration.ofMillis(100));
        assertThat(result).isPresent();

        // Cleanup
        result.ifPresent(scheduler::release);
        handles.subList(1, handles.size()).forEach(scheduler::release);
    }
}

