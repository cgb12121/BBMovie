package com.bbmovie.transcodeworker.service.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TranscodeScheduler.
 * Tests the weighted semaphore resource management.
 */
@DisplayName("TranscodeScheduler")
class TranscodeSchedulerTest {

    private TranscodeScheduler scheduler;

    @BeforeEach
    void setUp() {
        // Create scheduler with fixed capacity of 10 for predictable tests
        scheduler = new TranscodeScheduler(10);
    }

    @Nested
    @DisplayName("acquire()")
    class AcquireTests {

        @Test
        @DisplayName("should acquire resources when capacity available")
        void shouldAcquireWhenCapacityAvailable() throws InterruptedException {
            // When
            var handle = scheduler.acquire(4);

            // Then
            assertThat(handle).isNotNull();
            assertThat(handle.getCostWeight()).isEqualTo(4);
            assertThat(handle.getActualThreads()).isEqualTo(4);
            assertThat(scheduler.getCurrentUsage()).isEqualTo(4);
        }

        @Test
        @DisplayName("should clamp threads to maxCapacity when cost exceeds capacity")
        void shouldClampThreadsWhenCostExceedsCapacity() throws InterruptedException {
            // When - cost 32 exceeds maxCapacity 10
            var handle = scheduler.acquire(32);

            // Then
            assertThat(handle.getCostWeight()).isEqualTo(32);
            assertThat(handle.getActualThreads()).isEqualTo(10); // Clamped to maxCapacity
            assertThat(scheduler.getCurrentUsage()).isEqualTo(10);
        }

        @Test
        @DisplayName("should block when no capacity available")
        void shouldBlockWhenNoCapacity() throws InterruptedException {
            // Given - fill capacity
            scheduler.acquire(10);

            // When/Then - should timeout waiting for resources
            ExecutorService executor = Executors.newSingleThreadExecutor();
            CountDownLatch started = new CountDownLatch(1);
            AtomicInteger acquired = new AtomicInteger(0);

            executor.submit(() -> {
                try {
                    started.countDown();
                    var handle = scheduler.acquire(1); // Should block
                    acquired.set(1);
                    scheduler.release(handle);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            started.await();
            Thread.sleep(100); // Give time to potentially acquire

            assertThat(acquired.get()).isEqualTo(0); // Still blocked

            executor.shutdownNow();
        }
    }

    @Nested
    @DisplayName("tryAcquire()")
    class TryAcquireTests {

        @Test
        @DisplayName("should return handle when capacity available")
        void shouldReturnHandleWhenCapacityAvailable() {
            // When
            Optional<TranscodeScheduler.ResourceHandle> handle = 
                    scheduler.tryAcquire(4, Duration.ofMillis(100));

            // Then
            assertThat(handle).isPresent();
            assertThat(handle.get().getCostWeight()).isEqualTo(4);
        }

        @Test
        @DisplayName("should return empty when no capacity within timeout")
        void shouldReturnEmptyWhenNoCapacity() throws InterruptedException {
            // Given - fill capacity
            scheduler.acquire(10);

            // When
            long start = System.currentTimeMillis();
            Optional<TranscodeScheduler.ResourceHandle> handle = 
                    scheduler.tryAcquire(1, Duration.ofMillis(100));
            long elapsed = System.currentTimeMillis() - start;

            // Then
            assertThat(handle).isEmpty();
            assertThat(elapsed).isGreaterThanOrEqualTo(100); // Waited for timeout
        }

        @Test
        @DisplayName("should acquire immediately when released")
        void shouldAcquireWhenReleased() throws InterruptedException {
            // Given - fill capacity
            var firstHandle = scheduler.acquire(10);

            // When - release in another thread
            Executors.newSingleThreadExecutor().submit(() -> {
                try {
                    Thread.sleep(50);
                    scheduler.release(firstHandle);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            Optional<TranscodeScheduler.ResourceHandle> handle = 
                    scheduler.tryAcquire(5, Duration.ofMillis(200));

            // Then
            assertThat(handle).isPresent();
        }
    }

    @Nested
    @DisplayName("release()")
    class ReleaseTests {

        @Test
        @DisplayName("should release resources back to pool")
        void shouldReleaseResources() throws InterruptedException {
            // Given
            var handle = scheduler.acquire(5);
            assertThat(scheduler.getCurrentUsage()).isEqualTo(5);

            // When
            scheduler.release(handle);

            // Then
            assertThat(scheduler.getCurrentUsage()).isEqualTo(0);
            assertThat(scheduler.getAvailableCapacity()).isEqualTo(10);
        }

        @Test
        @DisplayName("should ignore null handle")
        void shouldIgnoreNullHandle() {
            // When/Then - should not throw
            assertThatCode(() -> scheduler.release(null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should ignore already released handle")
        void shouldIgnoreAlreadyReleasedHandle() throws InterruptedException {
            // Given
            var handle = scheduler.acquire(5);
            scheduler.release(handle);

            // When/Then - double release should not throw
            assertThatCode(() -> scheduler.release(handle)).doesNotThrowAnyException();
            assertThat(scheduler.getCurrentUsage()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Concurrent operations")
    class ConcurrentTests {

        @Test
        @DisplayName("should handle concurrent acquire/release correctly")
        void shouldHandleConcurrentOperations() throws InterruptedException {
            // Given
            int numThreads = 20;
            int iterations = 50;
            ExecutorService executor = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads * iterations);
            AtomicInteger successCount = new AtomicInteger(0);
            List<Throwable> errors = new ArrayList<>();

            // When - many threads acquiring/releasing
            for (int i = 0; i < numThreads * iterations; i++) {
                executor.submit(() -> {
                    try {
                        var handle = scheduler.acquire(1);
                        Thread.sleep(5); // Simulate work
                        scheduler.release(handle);
                        successCount.incrementAndGet();
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Then
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            assertThat(completed).isTrue();
            assertThat(errors).isEmpty();
            assertThat(successCount.get()).isEqualTo(numThreads * iterations);
            assertThat(scheduler.getCurrentUsage()).isEqualTo(0); // All released
        }
    }

    @Nested
    @DisplayName("Utility methods")
    class UtilityTests {

        @Test
        @DisplayName("canAcquire should return true when capacity available")
        void canAcquireShouldReturnTrue() {
            assertThat(scheduler.canAcquire(5)).isTrue();
            assertThat(scheduler.canAcquire(10)).isTrue();
        }

        @Test
        @DisplayName("canAcquire should return false when capacity insufficient")
        void canAcquireShouldReturnFalse() throws InterruptedException {
            scheduler.acquire(8);
            assertThat(scheduler.canAcquire(5)).isFalse();
            assertThat(scheduler.canAcquire(3)).isFalse();
            assertThat(scheduler.canAcquire(2)).isTrue();
        }

        @Test
        @DisplayName("getUsagePercentage should calculate correctly")
        void usagePercentageShouldBeCorrect() throws InterruptedException {
            assertThat(scheduler.getUsagePercentage()).isEqualTo(0.0);

            scheduler.acquire(5);
            assertThat(scheduler.getUsagePercentage()).isEqualTo(50.0);

            scheduler.acquire(5);
            assertThat(scheduler.getUsagePercentage()).isEqualTo(100.0);
        }
    }
}

