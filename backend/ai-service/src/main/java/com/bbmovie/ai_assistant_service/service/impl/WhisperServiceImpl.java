package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.config.whisper.WhisperNativeEngine;
import com.bbmovie.ai_assistant_service.service.WhisperService;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Whisper transcription service with production-grade memory safety.
 */
@Service
@RequiredArgsConstructor
public class WhisperServiceImpl implements WhisperService {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(WhisperServiceImpl.class);

    private final WhisperNativeEngine engine;

    // Safety limits: Reject files larger than 10MB to prevent OOM
    private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MIN_FILE_SIZE = 1024; // 1KB - minimum valid audio

    /**
     * Transcribes an audio file to text.
     *
     * @param audioFile Reactive multipart file from HTTP request
     * @return Mono<String> containing the transcribed text
     */
    @Override
    public Mono<String> transcribe(FilePart audioFile) {
        return readAndValidateAudioFile(audioFile)
                .flatMap(audioBytes -> {
                    log.info("Starting transcription. File: {}, Size: {} bytes", audioFile.filename(), audioBytes.length);

                    // Delegate to engine - it handles native context management
                    return engine.transcribe(audioBytes);
                })
                .onErrorMap(this::mapToUserFriendlyError)
                .doOnSuccess(transcript -> log.info("Transcription completed. File: {}, Text length: {} chars", audioFile.filename(), transcript.length()))
                .doOnError(error -> log.error("Transcription failed. File: {}, Error: {}", audioFile.filename(), error.getMessage()));
    }

    /**
     * Reads and validates an audio file from a reactive stream.
     * Ensures proper buffer release to prevent off-heap memory leaks.
     *
     * @param audioFile Multipart file part
     * @return Mono<byte[]> containing audio data
     */
    private Mono<byte[]> readAndValidateAudioFile(FilePart audioFile) {
        return DataBufferUtils.join(audioFile.content())
                .handle((dataBuffer, sink) -> {
                    try {
                        int size = dataBuffer.readableByteCount();

                        // Validate file size
                        if (size > MAX_IN_MEMORY_SIZE) {
                            sink.error(new ResponseStatusException(
                                    HttpStatus.PAYLOAD_TOO_LARGE,
                                    String.format("File too large: %.2f MB. Maximum allowed: 10 MB", size / 1024.0 / 1024.0)
                            ));
                            return;
                        }

                        if (size < MIN_FILE_SIZE) {
                            sink.error(new ResponseStatusException(
                                    HttpStatus.BAD_REQUEST,
                                    "File too small to be valid audio (minimum 1KB)"
                            ));
                            return;
                        }

                        // Read into a byte array
                        byte[] bytes = new byte[size];
                        dataBuffer.read(bytes);

                        log.debug("Audio file read successfully. Size: {} bytes", size);
                        sink.next(bytes);

                    } catch (Exception e) {
                        log.error("Failed to read audio file: {}", e.getMessage());
                        sink.error(new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Failed to read audio file: " + e.getMessage()
                        ));
                    } finally {
                        // CRITICAL: Always release buffer to prevent off-heap memory leak
                        DataBufferUtils.release(dataBuffer);
                    }
                });
    }

    /**
     * Maps technical exceptions to user-friendly HTTP errors.
     *
     * @param error Original exception
     * @return User-friendly ResponseStatusException
     */
    private Throwable mapToUserFriendlyError(Throwable error) {
        // Already a user-friendly error
        if (error instanceof ResponseStatusException) {
            return error;
        }

        // Queue full / system overloaded
        if (error instanceof RejectedExecutionException) {
            String message = error.getMessage();

            if (message != null && message.contains("Queue full")) {
                return new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Transcription service is currently overloaded. Please try again in 30 seconds."
                );
            }

            return new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "All transcription workers are busy. Please retry shortly."
            );
        }

        // Request timeout (audio too long or system slow)
        if (error instanceof TimeoutException) {
            return new ResponseStatusException(
                    HttpStatus.REQUEST_TIMEOUT,
                    "Transcription timed out. Audio file may be too long (>5 minutes) or in a complex format."
            );
        }

        // Audio format/conversion errors
        if (error.getMessage() != null) {
            String msg = error.getMessage().toLowerCase();

            if (msg.contains("convert") || msg.contains("format") || msg.contains("decode") || msg.contains("unsupported")) {
                return new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid or corrupted audio file. Supported formats: WAV, MP3, OGG, FLAC, M4A."
                );
            }

            if (msg.contains("out of memory") || msg.contains("oom")) {
                return new ResponseStatusException(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "Audio file too complex to process. Try a shorter file or lower quality."
                );
            }
        }

        // Generic internal error
        log.error("Unexpected transcription error", error);
        return new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal transcription error. Please contact support if this persists."
        );
    }

    /**
     * Gets current engine health status.
     * Useful for health checks and monitoring.
     *
     * @return Mono<EngineStatus> with detailed metrics
     */
    public Mono<EngineStatus> getStatus() {
        return Mono.fromCallable(() -> {
            var metrics = engine.getMetrics();

            // Calculate utilization percentages
            double contextUtilization = (double) metrics.contextsInUse() /
                    metrics.poolSize() * 100;
            double queueFullness = (double) metrics.queuedTasks() /
                    metrics.maxQueueSize() * 100;

            // Determine health level
            HealthLevel health;
            if (!metrics.isHealthy()) {
                health = HealthLevel.DEGRADED;
            } else if (contextUtilization > 80 || queueFullness > 60) {
                health = HealthLevel.WARNING;
            } else {
                health = HealthLevel.HEALTHY;
            }

            return new EngineStatus(
                    health,
                    metrics.poolSize(),
                    metrics.contextsInUse(),
                    contextUtilization,
                    metrics.activeInferences(),
                    metrics.queuedTasks(),
                    metrics.maxQueueSize(),
                    queueFullness,
                    (int) metrics.totalProcessed(),
                    (int) metrics.totalRejected(),
                    calculateThroughput(metrics)
            );
        });
    }

    public boolean isHealthy() {
        return engine.isHealthy();
    }

    public double getUtilizationPercent() {
        var metrics = engine.getMetrics();
        return metrics.getUtilizationPercent();
    }

    /**
     * Calculates current throughput (requests per minute).
     */
    private double calculateThroughput(WhisperNativeEngine.EngineMetrics metrics) {
        // Simple calculation based on uptime
        // In production, use a sliding window or metrics library
        long uptimeMinutes = java.lang.management.ManagementFactory
                .getRuntimeMXBean().getUptime() / 60000;

        if (uptimeMinutes == 0) return 0;
        return (double) metrics.totalProcessed() / uptimeMinutes;
    }

    /**
     * Health level indicator for monitoring.
     */
    public enum HealthLevel {
        HEALTHY,    // System operating normally
        WARNING,    // High utilization but still functional
        DEGRADED    // System overloaded, rejecting requests
    }

    /**
     * Detailed engine status for monitoring endpoints.
     */
    public record EngineStatus(
            HealthLevel health,
            int poolSize,
            int contextsInUse,
            double contextUtilizationPercent,
            int activeInferences,
            int queuedTasks,
            int maxQueueSize,
            double queueFullnessPercent,
            int totalProcessed,
            int totalRejected,
            double throughputPerMinute
    ) {
        public String getHealthMessage() {
            return switch (health) {
                case HEALTHY -> "System operating normally";
                case WARNING -> "High load detected - consider scaling";
                case DEGRADED -> "System overloaded - requests being rejected";
            };
        }
    }
}