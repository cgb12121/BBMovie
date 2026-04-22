package com.bbmovie.transcodeworker.service.pipeline.queue;

import com.bbmovie.transcodeworker.enums.UploadPurpose;
import com.bbmovie.transcodeworker.service.pipeline.dto.ExecuteTask;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeResult;
import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for PipelineQueues.
 * Tests the blocking queue behavior for pipeline stages.
 */
@DisplayName("PipelineQueues")
class PipelineQueuesTest {

    private PipelineQueues pipelineQueues;

    @BeforeEach
    void setUp() {
        pipelineQueues = new PipelineQueues();
        // Set small capacities for testing
        ReflectionTestUtils.setField(pipelineQueues, "probeQueueCapacity", 5);
        ReflectionTestUtils.setField(pipelineQueues, "executeQueueCapacity", 3);
        pipelineQueues.init();
    }

    private ProbeTask createProbeTask(String key) {
        return ProbeTask.create(
                mock(io.nats.client.Message.class),
                "test-bucket",
                key,
                UploadPurpose.MOVIE_SOURCE,
                "upload-123",
                "video/mp4",
                1024L
        );
    }

    private ExecuteTask createExecuteTask(String key) {
        ProbeTask probeTask = createProbeTask(key);
        ProbeResult probeResult = ProbeResult.forVideo(1920, 1080, 120.0, "h264",
                List.of("1080p", "720p"), 32, 48);
        return ExecuteTask.from(probeTask, probeResult, null);
    }

    @Nested
    @DisplayName("Probe Queue")
    class ProbeQueueTests {

        @Test
        @DisplayName("should add and retrieve probe tasks in FIFO order")
        void shouldAddAndRetrieveFIFO() throws InterruptedException {
            // Given
            ProbeTask task1 = createProbeTask("video1.mp4");
            ProbeTask task2 = createProbeTask("video2.mp4");

            // When
            pipelineQueues.putProbeTask(task1);
            pipelineQueues.putProbeTask(task2);

            // Then
            ProbeTask retrieved1 = pipelineQueues.takeProbeTask();
            ProbeTask retrieved2 = pipelineQueues.takeProbeTask();

            assertThat(retrieved1.key()).isEqualTo("video1.mp4");
            assertThat(retrieved2.key()).isEqualTo("video2.mp4");
        }

        @Test
        @DisplayName("should block when queue is full")
        void shouldBlockWhenFull() throws InterruptedException {
            // Given - fill the queue (capacity = 5)
            for (int i = 0; i < 5; i++) {
                pipelineQueues.putProbeTask(createProbeTask("video" + i + ".mp4"));
            }

            // When/Then - next put should block
            CountDownLatch started = new CountDownLatch(1);
            AtomicInteger added = new AtomicInteger(0);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(() -> {
                try {
                    started.countDown();
                    pipelineQueues.putProbeTask(createProbeTask("blocked.mp4"));
                    added.set(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            started.await();
            Thread.sleep(50);

            assertThat(added.get()).isEqualTo(0); // Still blocked
            assertThat(pipelineQueues.getProbeQueueSize()).isEqualTo(5);

            executor.shutdownNow();
        }

        @Test
        @DisplayName("should offer with timeout and return false when full")
        void shouldOfferWithTimeoutReturnFalse() throws InterruptedException {
            // Given - fill the queue
            for (int i = 0; i < 5; i++) {
                pipelineQueues.putProbeTask(createProbeTask("video" + i + ".mp4"));
            }

            // When
            long start = System.currentTimeMillis();
            boolean result = pipelineQueues.offerProbeTask(
                    createProbeTask("extra.mp4"), 100, TimeUnit.MILLISECONDS);
            long elapsed = System.currentTimeMillis() - start;

            // Then
            assertThat(result).isFalse();
            assertThat(elapsed).isGreaterThanOrEqualTo(100);
        }

        @Test
        @DisplayName("should poll with timeout and return null when empty")
        void shouldPollWithTimeoutReturnNull() throws InterruptedException {
            // When
            long start = System.currentTimeMillis();
            ProbeTask result = pipelineQueues.pollProbeTask(100, TimeUnit.MILLISECONDS);
            long elapsed = System.currentTimeMillis() - start;

            // Then
            assertThat(result).isNull();
            assertThat(elapsed).isGreaterThanOrEqualTo(100);
        }
    }

    @Nested
    @DisplayName("Execute Queue")
    class ExecuteQueueTests {

        @Test
        @DisplayName("should add and retrieve execute tasks")
        void shouldAddAndRetrieve() throws InterruptedException {
            // Given
            ExecuteTask task = createExecuteTask("video.mp4");

            // When
            pipelineQueues.putExecuteTask(task);
            ExecuteTask retrieved = pipelineQueues.takeExecuteTask();

            // Then
            assertThat(retrieved.key()).isEqualTo("video.mp4");
            assertThat(retrieved.probeResult().width()).isEqualTo(1920);
        }

        @Test
        @DisplayName("should track queue size correctly")
        void shouldTrackQueueSize() throws InterruptedException {
            assertThat(pipelineQueues.getExecuteQueueSize()).isEqualTo(0);

            pipelineQueues.putExecuteTask(createExecuteTask("v1.mp4"));
            assertThat(pipelineQueues.getExecuteQueueSize()).isEqualTo(1);

            pipelineQueues.putExecuteTask(createExecuteTask("v2.mp4"));
            assertThat(pipelineQueues.getExecuteQueueSize()).isEqualTo(2);

            pipelineQueues.takeExecuteTask();
            assertThat(pipelineQueues.getExecuteQueueSize()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Monitoring")
    class MonitoringTests {

        @Test
        @DisplayName("should report remaining capacity correctly")
        void shouldReportRemainingCapacity() throws InterruptedException {
            assertThat(pipelineQueues.getProbeQueueRemainingCapacity()).isEqualTo(5);

            pipelineQueues.putProbeTask(createProbeTask("v1.mp4"));
            assertThat(pipelineQueues.getProbeQueueRemainingCapacity()).isEqualTo(4);
        }

        @Test
        @DisplayName("should report empty status correctly")
        void shouldReportEmptyStatus() throws InterruptedException {
            assertThat(pipelineQueues.isProbeQueueEmpty()).isTrue();
            assertThat(pipelineQueues.isExecuteQueueEmpty()).isTrue();

            pipelineQueues.putProbeTask(createProbeTask("v.mp4"));
            assertThat(pipelineQueues.isProbeQueueEmpty()).isFalse();
        }

        @Test
        @DisplayName("should clear both queues")
        void shouldClearBothQueues() throws InterruptedException {
            pipelineQueues.putProbeTask(createProbeTask("v1.mp4"));
            pipelineQueues.putExecuteTask(createExecuteTask("v2.mp4"));

            pipelineQueues.clear();

            assertThat(pipelineQueues.getProbeQueueSize()).isEqualTo(0);
            assertThat(pipelineQueues.getExecuteQueueSize()).isEqualTo(0);
        }
    }
}

