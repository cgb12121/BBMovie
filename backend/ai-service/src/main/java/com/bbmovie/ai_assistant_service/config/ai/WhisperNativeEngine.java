package com.bbmovie.ai_assistant_service.config.ai;

import com.bbmovie.ai_assistant_service.utils.AudioConverterUtils;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.ContextView;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.lang.ref.Cleaner;

/**
 * 1. NO ThreadLocal (causes native memory leaks with whisper.cpp)
 * 2. Explicit context lifecycle management
 * 3. Cleaner API for guaranteed native resource cleanup
 * 4. Context propagation for reactive chains
 * 5. Pool of native contexts instead of per-thread
 */
@Component
public class WhisperNativeEngine implements AutoCloseable {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(WhisperNativeEngine.class);

    private final WhisperJNI whisper = new WhisperJNI();
    private final WhisperProperties properties;
    private final ResourceLoader resourceLoader;

    // Pool of native contexts (EXPLICIT lifecycle management)
    private final BlockingQueue<NativeContextWrapper> contextPool;

    // Worker pool for CPU-intensive audio conversion
    private final ExecutorService conversionPool;

    // Inference executor (bounded by context pool size)
    private final ExecutorService inferencePool;

    // Request queue
    private final BlockingQueue<TranscriptionTask> taskQueue;

    // Java 9+ Cleaner for guaranteed native cleanup
    private final Cleaner cleaner = Cleaner.create();

    // Shutdown flag
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    // Metrics
    private final AtomicInteger activeInferences = new AtomicInteger(0);
    private final AtomicInteger totalProcessed = new AtomicInteger(0);
    private final AtomicInteger totalRejected = new AtomicInteger(0);

    // Configuration
    private final int poolSize;
    private final int maxQueueSize;
    private static final long INFERENCE_TIMEOUT_MS = 300_000;
    private static final long CONTEXT_ACQUIRE_TIMEOUT_MS = 30_000;

    public WhisperNativeEngine(WhisperProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;

        // Pool size = number of parallel inferences allowed
        this.poolSize = properties.getWorkerThreads() > 0
                ? properties.getWorkerThreads()
                : Math.max(2, Runtime.getRuntime().availableProcessors() / 2);

        this.maxQueueSize = properties.getMaxQueueSize() > 0
                ? properties.getMaxQueueSize()
                : poolSize * 3;

        log.info("Initializing Whisper engine: {} contexts, queue size {}",
                poolSize, maxQueueSize);

        // Initialize context pool (BLOCKING - do this in PostConstruct)
        this.contextPool = new LinkedBlockingQueue<>(poolSize);

        // Task queue
        this.taskQueue = new LinkedBlockingQueue<>(maxQueueSize);

        // Conversion pool (more threads OK since it's just CPU work)
        this.conversionPool = Executors.newFixedThreadPool(
                poolSize * 2,
                new NamedThreadFactory("whisper-convert")
        );

        // Inference pool (matches context pool size)
        this.inferencePool = Executors.newFixedThreadPool(
                poolSize,
                new NamedThreadFactory("whisper-infer")
        );
    }

    /**
     * Initialize native contexts eagerly at startup.
     * CRITICAL: Must happen BEFORE any requests arrive.
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing {} native Whisper contexts...", poolSize);

        try {
            WhisperJNI.loadLibrary();
            Path modelPath = loadModelFromClasspath();

            for (int i = 0; i < poolSize; i++) {
                WhisperContext nativeCtx = whisper.init(modelPath);
                if (nativeCtx == null) {
                    throw new RuntimeException("Failed to init native context " + i);
                }

                // Wrap with cleanup registration
                NativeContextWrapper wrapper = new NativeContextWrapper(nativeCtx, i);

                // Register cleanup with Java Cleaner (guaranteed to run)
                cleaner.register(wrapper, new ContextCleanupAction(whisper, nativeCtx, i));

                boolean ignored = contextPool.offer(wrapper);
                log.info("Initialized native context #{}", i);
            }

            log.info("All native contexts initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize Whisper engine", e);
            throw new RuntimeException("Whisper initialization failed", e);
        }
    }

    /**
     * Transcribes audio bytes to text with FULL REACTIVE CONTEXT PROPAGATION.
     * Context is preserved throughout the entire chain.
     *
     * @param rawAudioBytes Raw audio file bytes (WAV, MP3, OGG, etc.)
     * @return Mono<String> containing transcribed text
     * @throws IllegalStateException if the engine is shutdown
     * @throws RejectedExecutionException if the queue is full
     */
    public Mono<String> transcribe(byte[] rawAudioBytes) {
        if (isShutdown.get()) {
            return Mono.error(new IllegalStateException("Engine is shutdown"));
        }

        // Fail-fast: check queue capacity BEFORE any processing
        if (taskQueue.remainingCapacity() == 0) {
            totalRejected.incrementAndGet();
            return Mono.error(new RejectedExecutionException(
                    String.format("Queue full (%d/%d)", taskQueue.size(), maxQueueSize)
            ));
        }

        // Create a task with context capture
        return Mono.deferContextual(contextView -> {
            TranscriptionTask task = new TranscriptionTask(rawAudioBytes, contextView);

            // Enqueue
            if (!taskQueue.offer(task)) {
                totalRejected.incrementAndGet();
                return Mono.error(new RejectedExecutionException("Queue full"));
            }

            // Process asynchronously
            CompletableFuture.runAsync(() -> processTask(task), inferencePool);

            // Return Mono that completes when a task finishes
            return task.result.asMono()
                    .timeout(java.time.Duration.ofMillis(INFERENCE_TIMEOUT_MS))
                    .contextWrite(contextView); // Propagate context downstream
        });
    }

    /**
     * Processes a single task with proper native memory management.
     */
    private void processTask(TranscriptionTask task) {
        NativeContextWrapper contextWrapper = null;

        try {
            // Step 1: Convert audio (expensive CPU work)
            // Do this OUTSIDE the native context acquisition to save resources
            float[] audioData = convertAudio(task.audioBytes);

            // Step 2: Acquire native context from the pool (blocking with timeout)
            contextWrapper = contextPool.poll(CONTEXT_ACQUIRE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (contextWrapper == null) {
                throw new TimeoutException("Failed to acquire native context within timeout");
            }

            activeInferences.incrementAndGet();

            // Step 3: Run inference with acquired context
            String result = runInference(contextWrapper.context, audioData);

            totalProcessed.incrementAndGet();

            // Step 4: Emit result WITH reactive context
            task.result.emitValue(result, Sinks.EmitFailureHandler.FAIL_FAST);

        } catch (Exception e) {
            log.error("Inference failed", e);
            task.result.emitError(e, Sinks.EmitFailureHandler.FAIL_FAST);

        } finally {
            activeInferences.decrementAndGet();

            // CRITICAL: Return context to pool (even on error)
            if (contextWrapper != null) {
                if (!contextPool.offer(contextWrapper)) {
                    log.error("Failed to return context #{} to pool!", contextWrapper.id);
                    // This should NEVER happen - it means we have a leak
                }
            }
        }
    }

    /**
     * Converts audio bytes to a float array (CPU-intensive, no native resources).
     */
    private float[] convertAudio(byte[] audioBytes) throws Exception {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(audioBytes)) {
            return AudioConverterUtils.convertToWhisperFormat(bis);
        }
    }

    /**
     * Runs native inference with explicit context.
     * NO LOCKS NEEDED - each context is used by only one thread at a time.
     */
    private String runInference(WhisperContext ctx, float[] audioData) {
        WhisperFullParams params = new WhisperFullParams();
        params.language = null; // Auto-detect
        params.translate = false;
        params.printProgress = false;

        int code = whisper.full(ctx, params, audioData, audioData.length);
        if (code != 0) {
            throw new RuntimeException("Inference failed with code: " + code);
        }

        StringBuilder result = new StringBuilder();
        int numSegments = whisper.fullNSegments(ctx);
        for (int i = 0; i < numSegments; i++) {
            result.append(whisper.fullGetSegmentText(ctx, i)).append(" ");
        }

        return result.toString().trim();
    }

    /**
     * Loads model from the classpath to a temp file.
     */
    private Path loadModelFromClasspath() throws IOException {
        Resource resource = resourceLoader.getResource(properties.getModelPath());
        if (!resource.exists()) {
            throw new IOException("Model not found: " + properties.getModelPath());
        }

        Path tempFile = Files.createTempFile("whisper", ".bin");
        try (InputStream is = resource.getInputStream()) {
            Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        tempFile.toFile().deleteOnExit();
        log.info("Model loaded to: {}", tempFile);
        return tempFile;
    }

    /**
     * Health metrics for monitoring.
     */
    public EngineMetrics getMetrics() {
        return new EngineMetrics(
                poolSize,
                poolSize - contextPool.size(), // Contexts in use
                activeInferences.get(),
                taskQueue.size(),
                maxQueueSize,
                totalProcessed.get(),
                totalRejected.get()
        );
    }

    /**
     * Graceful shutdown with native resource cleanup.
     */
    @PreDestroy
    @Override
    public void close() {
        if (isShutdown.compareAndSet(false, true)) {
            log.info("Shutting down Whisper engine...");

            // Stop accepting new tasks
            conversionPool.shutdown();
            inferencePool.shutdown();

            try {
                // Wait for in-flight tasks
                if (!inferencePool.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.warn("Inference pool did not terminate gracefully");
                    inferencePool.shutdownNow();
                }
                if (!conversionPool.awaitTermination(10, TimeUnit.SECONDS)) {
                    conversionPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Free all native contexts
            log.info("Freeing {} native contexts...", contextPool.size());
            NativeContextWrapper wrapper;
            while ((wrapper = contextPool.poll()) != null) {
                try {
                    whisper.free(wrapper.context);
                    log.debug("Freed native context #{}", wrapper.id);
                } catch (Exception e) {
                    log.error("Error freeing context #{}", wrapper.id, e);
                }
            }

            log.info("Whisper engine shutdown complete");
        }
    }

    /**
     * Wrapper for native context with ID for debugging.
     */
    private record NativeContextWrapper(WhisperContext context, int id) {
    }

    /**
     * Cleaner action for guaranteed native cleanup (last resort).
     */
    private record ContextCleanupAction(WhisperJNI whisper, WhisperContext context, int id) implements Runnable {

        @Override
        public void run() {
            // This runs when NativeContextWrapper is GC'd
            // Should NOT happen in normal operation (indicates leak)
            try {
                whisper.free(context);
                System.err.println("WARNING: Native context #" + id + " was cleaned up by GC! This indicates a resource leak.");
            } catch (Exception e) {
                System.err.println("Failed to cleanup leaked context #" + id);
            }
        }
    }

    /**
     * Task with reactive context capture.
     */
    private static class TranscriptionTask {
        final byte[] audioBytes;
        final ContextView reactiveContext;
        final Sinks.One<String> result;

        TranscriptionTask(byte[] audioBytes, ContextView reactiveContext) {
            this.audioBytes = audioBytes;
            this.reactiveContext = reactiveContext;
            this.result = Sinks.one();
        }
    }

    /**
     * Named thread factory for debugging.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String prefix;

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread t = new Thread(runnable, prefix + "-" + counter.incrementAndGet());
            t.setDaemon(false);
            return t;
        }
    }

    /**
     * Metrics DTO.
     */
    public record EngineMetrics(
            int poolSize,
            int contextsInUse,
            int activeInferences,
            int queuedTasks,
            int maxQueueSize,
            int totalProcessed,
            int totalRejected
    ) {
        public boolean isHealthy() {
            return contextsInUse < poolSize && queuedTasks < maxQueueSize * 0.8;
        }
    }
}