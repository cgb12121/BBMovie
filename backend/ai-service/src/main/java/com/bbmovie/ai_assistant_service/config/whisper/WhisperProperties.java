package com.bbmovie.ai_assistant_service.config.whisper;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Whisper transcription engine.
 * <p>
 * Usage in application.yml:
 * <pre>
 * whisper:
 *   model-path: classpath:models/ggml-base.bin
 *   worker-threads: 2
 *   max-queue-size: 100
 * </pre>
 */
@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "whisper")
public class WhisperProperties {

    /**
     * Path to a Whisper model file.
     * Supports classpath: and file: prefixes.
     * <p>
     * Model size guide:
     * - ggml-tiny.bin: ~75MB, fastest, less accurate
     * - ggml-base.bin: ~140MB, balanced (recommended)
     * - ggml-small.bin: ~460MB, more accurate, slower
     * - ggml-medium.bin: ~1.5GB, high accuracy, much slower
     */
    private String modelPath = "classpath:models/ggml-tiny-q5_1.bin";

    /**
     * Number of concurrent native contexts (worker threads).
     * <p>
     * Recommendations:
     * - Small server (4GB RAM): 1-2 workers
     * - Medium server (8GB RAM): 2-4 workers
     * - Large server (16GB+ RAM): 4-8 workers
     * <p>
     * IMPORTANT: Each context consumes significant RAM:
     * - tiny model: ~500MB per context
     * - base model: ~1GB per context
     * - small model: ~2GB per context
     * <p>
     * Formula: workerThreads * modelSize * 1.5 < availableRAM
     */
    private int workerThreads = 2;

    /**
     * Maximum logical queue size for monitoring.
     * Note: Apache Commons Pool manages actual queue via maxWait timeout.
     * This value is used for metrics and health checks.
     */
    private int maxQueueSize = 100;

    /**
     * Whether to eagerly initialize native contexts at startup.
     * If false, contexts are allocated lazily on the first request.
     * <p>
     * Eager init pros:
     * - First request is fast
     * - Detects memory issues at startup
     * <p>
     * Lazy init pros:
     * - Faster application startup
     * - Saves memory if transcription rarely used
     */
    private boolean eagerInit = false;

    /**
     * Inference timeout in seconds.
     * Protects against hung requests.
     */
    private long inferenceTimeoutSeconds = 300; // 5 minutes

    /**
     * Pool wait timeout in seconds.
     * How long to wait for an available context before rejecting the request.
     */
    private long poolWaitTimeoutSeconds = 60;
}
