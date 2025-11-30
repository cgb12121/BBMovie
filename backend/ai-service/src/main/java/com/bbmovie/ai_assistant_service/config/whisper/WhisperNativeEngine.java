package com.bbmovie.ai_assistant_service.config.whisper;

import com.bbmovie.ai_assistant_service.utils.AudioConverterUtils;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Native Whisper engine.
 * <p>
 * Key Features:
 * - Lazy loading: Models allocated on-demand
 * - Automatic native memory management via pooling
 * - Graceful degradation under load
 * - Comprehensive metrics tracking
 * <p>
 * Memory Safety:
 * - Pool automatically calls destroyObject() when contexts are evicted
 * - No manual cleanup needed (except @PreDestroy)
 * - Invalidates corrupted contexts to prevent reuse
 */
@Component
public class WhisperNativeEngine implements AutoCloseable {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(WhisperNativeEngine.class);

    private final WhisperJNI whisper = new WhisperJNI();
    private final WhisperProperties properties;
    private final ResourceLoader resourceLoader;

    // Apache Commons Pool for native context management
    private GenericObjectPool<WhisperContext> whisperPool;

    // Cached model path (extracted from classpath once)
    private Path cachedModelPath;

    // Executor for async inference
    private final ExecutorService executorService;

    // Shutdown flag
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // Metrics counters
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalRejected = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    // Inference timeout configuration
    private static final long INFERENCE_TIMEOUT_SECONDS = 300; // 5 minutes

    @Autowired
    public WhisperNativeEngine(WhisperProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;

        // Thread pool matches max concurrent contexts
        int maxThreads = properties.getWorkerThreads() > 0
                ? properties.getWorkerThreads()
                : 2;

        this.executorService = Executors.newFixedThreadPool(maxThreads, r -> {
            Thread t = new Thread(r, "whisper-worker");
            t.setDaemon(false); // Non-daemon for graceful shutdown
            return t;
        });
    }

    /**
     * Initializes the engine and prepares the object pool.
     * Models are NOT loaded until the first transcription request (lazy).
     */
    @PostConstruct
    public void init() throws IOException {
        log.info("Initializing Whisper Engine with Apache Commons Pool 2...");

        try {
            WhisperJNI.loadLibrary();
            log.info("Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            log.warn("Native library load error (might already be loaded): {}", e.getMessage());
        }

        // Extract model from a classpath to a temp file (once)
        this.cachedModelPath = loadModelFromClasspath();
        log.info("Model prepared at: {}", cachedModelPath);

        // Configure object pool factory
        BasePooledObjectFactory<WhisperContext> factory = new WhisperContextFactory();

        GenericObjectPoolConfig<WhisperContext> config = new GenericObjectPoolConfig<>();
        int maxPoolSize = properties.getWorkerThreads() > 0
                ? properties.getWorkerThreads()
                : 2;

        // Pool configuration
        config.setMaxTotal(maxPoolSize);     // Max concurrent contexts
        config.setMaxIdle(maxPoolSize);      // Keep all contexts ready when loaded
        config.setMinIdle(0);                // Start lazy: 0 contexts allocated

        // Blocking behavior when pool exhausted
        config.setBlockWhenExhausted(true);
        config.setMaxWait(Duration.ofSeconds(60));

        // Test on borrow to detect corrupted contexts
        config.setTestOnBorrow(false); // Whisper doesn't have a fast health check
        config.setTestOnReturn(false);

        // Eviction policy (optional for long-running apps)
        config.setTimeBetweenEvictionRuns(Duration.ofMinutes(10));
        config.setMinEvictableIdleDuration(Duration.ofMinutes(30));

        this.whisperPool = new GenericObjectPool<>(factory, config);

        log.info("Whisper Pool initialized. Max contexts: {}, Queue size: {}",
                maxPoolSize, properties.getMaxQueueSize());
    }

    /**
     * Transcribes audio bytes to text.
     * Returns a Mono that completes when transcription finishes.
     *
     * @param rawAudioBytes Audio file bytes (WAV, MP3, OGG, etc.)
     * @return Mono<String> containing transcribed text
     * @throws IllegalStateException if the engine is shutting down
     * @throws NoSuchElementException if pool wait time exceeded
     * @sneaky-throw {@link TimeoutException} if inference exceeds timeout
     */
    public Mono<String> transcribe(byte[] rawAudioBytes) {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("Service is shutting down"));
        }

        // Bridge async execution to reactive stream
        Sinks.One<String> sink = Sinks.one();

        // Submit to executor with timeout
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            WhisperContext ctx = null;
            boolean contextBorrowed = false;

            try {
                // Step 1: Convert audio (CPU-intensive, no native resources yet)
                float[] audioData;
                try (ByteArrayInputStream bis = new ByteArrayInputStream(rawAudioBytes)) {
                    audioData = AudioConverterUtils.convertToWhisperFormat(bis);
                }

                log.debug("Audio converted. Sample count: {}", audioData.length);

                // Step 2: Borrow context from pool
                // This may block if the pool is exhausted (max 60s by default)
                // Triggers lazy allocation if the pool is empty
                ctx = whisperPool.borrowObject();
                contextBorrowed = true;

                log.debug("Context borrowed. Active: {}, Idle: {}",
                        whisperPool.getNumActive(), whisperPool.getNumIdle());

                // Step 3: Run inference
                String result = runInference(ctx, audioData);

                // Success
                totalProcessed.incrementAndGet();
                sink.tryEmitValue(result);

            } catch (NoSuchElementException e) {
                // Pool wait timeout exceeded
                totalRejected.incrementAndGet();
                log.warn("Failed to borrow context (pool exhausted): {}", e.getMessage());
                sink.tryEmitError(new TimeoutException(
                        "All workers busy. Pool wait timeout exceeded."
                ));

            } catch (Exception e) {
                totalErrors.incrementAndGet();
                log.error("Transcription error: {}", e.getMessage(), e);

                // If context was corrupted by native error, invalidate it
                if (contextBorrowed && ctx != null) {
                    try {
                        log.warn("Invalidating potentially corrupted context");
                        whisperPool.invalidateObject(ctx);
                        ctx = null; // Prevent return to the pool
                    } catch (Exception invalidateError) {
                        log.error("Failed to invalidate context", invalidateError);
                    }
                }

                sink.tryEmitError(e);

            } finally {
                // Step 4: Return context to the pool for reuse
                if (contextBorrowed && ctx != null) {
                    whisperPool.returnObject(ctx);
                    log.debug("Context returned to pool");
                }
            }
        }, executorService);

        // Add timeout to prevent hung requests
        return sink.asMono()
                .timeout(Duration.ofSeconds(INFERENCE_TIMEOUT_SECONDS))
                .doOnError(TimeoutException.class, e -> {
                    log.error("Inference timeout after {}s", INFERENCE_TIMEOUT_SECONDS);
                    future.cancel(true); // Attempt to cancel
                });
    }

    /**
     * Runs native inference with the given context.
     *
     * @param ctx Native Whisper context (borrowed from pool)
     * @param audioData Audio samples as a float array
     * @return Transcribed text
     * @throws IOException if native inference fails
     */
    private String runInference(WhisperContext ctx, float[] audioData) throws IOException {
        WhisperFullParams params = new WhisperFullParams();
        params.language = null;      // Auto-detect language
        params.translate = false;    // Keep the original language
        params.printProgress = false;

        // Call native method
        int code = whisper.full(ctx, params, audioData, audioData.length);
        if (code != 0) {
            throw new IOException("Native inference failed with code: " + code);
        }

        // Extract segments
        StringBuilder result = new StringBuilder();
        int numSegments = whisper.fullNSegments(ctx);
        for (int i = 0; i < numSegments; i++) {
            result.append(whisper.fullGetSegmentText(ctx, i)).append(" ");
        }

        return result.toString().trim();
    }

    /**
     * Loads model from the classpath and extracts to a temporary file.
     * Required because whisper.cpp needs a physical file path.
     *
     * @return Path to an extracted model file
     * @throws IOException if the model isn't found or extraction fails
     */
    private Path loadModelFromClasspath() throws IOException {
        Resource resource = resourceLoader.getResource(properties.getModelPath());
        if (!resource.exists()) {
            throw new IOException("Whisper model not found at: " + properties.getModelPath());
        }

        // Create a temp file
        Path tempFile = Files.createTempFile("whisper-model-", ".bin");
        tempFile.toFile().deleteOnExit();

        // Extract model
        try (InputStream is = resource.getInputStream()) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Model extracted: {} ({} bytes)",
                tempFile, Files.size(tempFile));

        return tempFile;
    }

    /**
     * Returns current engine metrics for monitoring.
     *
     * @return EngineMetrics snapshot
     */
    public EngineMetrics getMetrics() {
        if (whisperPool == null) {
            return new EngineMetrics(0, 0, 0, 0, 0, 0, 0, 0);
        }

        int maxTotal = whisperPool.getMaxTotal();
        int active = whisperPool.getNumActive();
        int idle = whisperPool.getNumIdle();
        int waiters = whisperPool.getNumWaiters();

        return new EngineMetrics(
                maxTotal,                    // poolSize
                active,                      // contextsInUse
                active,                      // activeInferences (same as contexts in use)
                idle,                        // idle in the pool
                waiters,                     // queuedTasks (threads waiting)
                properties.getMaxQueueSize(), // maxQueueSize (logical limit)
                totalProcessed.get(),        // totalProcessed
                totalRejected.get()          // totalRejected
        );
    }

    /**
     * Checks if the engine is healthy and can accept requests.
     *
     * @return true if healthy
     */
    public boolean isHealthy() {
        if (whisperPool == null || isShutdown.get()) {
            return false;
        }

        int active = whisperPool.getNumActive();
        int maxTotal = whisperPool.getMaxTotal();
        int waiters = whisperPool.getNumWaiters();

        // Healthy if not fully saturated
        return active < maxTotal || waiters == 0;
    }

    /**
     * Graceful shutdown: Waits for in-flight tasks and frees native resources.
     */
    @PreDestroy
    @Override
    public void close() {
        if (isShutdown.compareAndSet(false, true)) {
            log.info("Shutting down Whisper Engine...");

            // Stop accepting new tasks
            executorService.shutdown();

            try {
                // Wait for in-flight tasks (max 30s)
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("Executor did not terminate gracefully, forcing shutdown");
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }

            // Close pool (automatically calls destroyObject for all contexts)
            if (whisperPool != null) {
                log.info("Closing pool. Active: {}, Idle: {}",
                        whisperPool.getNumActive(), whisperPool.getNumIdle());
                whisperPool.close();
            }

            log.info("Whisper Engine shutdown complete. Processed: {}, Rejected: {}, Errors: {}",
                    totalProcessed.get(), totalRejected.get(), totalErrors.get());
        }
    }

    /**
     * Factory for creating and destroying WhisperContext instances.
     */
    private class WhisperContextFactory extends BasePooledObjectFactory<WhisperContext> {

        @Override
        public WhisperContext create() throws Exception {
            log.debug("ALLOCATING new native Whisper context (lazy init)");
            log.debug("Current memory - Heap: {} MB / {} MB",
                    Runtime.getRuntime().totalMemory() / 1024 / 1024,
                    Runtime.getRuntime().maxMemory() / 1024 / 1024);

            WhisperContext ctx = whisper.init(cachedModelPath);
            if (ctx == null) {
                throw new OutOfMemoryError(
                        "WhisperJNI.init() returned null. Likely out of native memory!"
                );
            }

            log.info("Native context allocated successfully");
            return ctx;
        }

        @Override
        public PooledObject<WhisperContext> wrap(WhisperContext ctx) {
            return new DefaultPooledObject<>(ctx);
        }

        @Override
        public void destroyObject(PooledObject<WhisperContext> pooledObject) {
            log.info("FREEING native Whisper context");
            try {
                whisper.free(pooledObject.getObject());
                log.debug("Context freed successfully");
            } catch (Exception e) {
                log.error("Error freeing context", e);
            }
        }
    }

    /**
     * Metrics record for monitoring.
     */
    public record EngineMetrics(
            int poolSize,
            int contextsInUse,
            int activeInferences,
            int idleInferences,
            int queuedTasks,
            int maxQueueSize,
            long totalProcessed,
            long totalRejected
    ) {
        public boolean isHealthy() {
            return contextsInUse < poolSize && queuedTasks < maxQueueSize * 0.8;
        }

        public double getUtilizationPercent() {
            return poolSize > 0 ? (double) contextsInUse / poolSize * 100 : 0;
        }
    }
}