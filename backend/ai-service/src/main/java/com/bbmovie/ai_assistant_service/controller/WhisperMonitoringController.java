package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.service.impl.WhisperServiceImpl;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * Monitoring endpoints for Whisper engine health and native memory usage.
 * <p>
 * CRITICAL: Use these endpoints to detect native memory leaks!
 * <P>
 * This will be moved to a separate microservice in the future
 */
@RestController
@RequestMapping("/admin/whisper")
@RequiredArgsConstructor
public class WhisperMonitoringController {

    private final WhisperServiceImpl whisperService;

    /**
     * Detailed engine status.
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<WhisperServiceImpl.EngineStatus>> getStatus() {
        return whisperService.getStatus().map(ResponseEntity::ok);
    }

    /**
     * Memory metrics (both JVM heap and native).
     * <p>
     * WARNING: If "native memory" keeps growing but "contexts in use" is 0,
     * you have a native memory leak!
     */
    @GetMapping("/memory")
    public Mono<ResponseEntity<MemoryMetrics>> getMemoryMetrics() {
        return Mono.fromCallable(() -> {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
            
            // Get native memory (approximation via Direct Buffers)
            long directMemoryUsed = 0;
            try {
                var pools = ManagementFactory.getMemoryPoolMXBeans();
                for (var pool : pools) {
                    if (pool.getName().contains("Direct")) {
                        directMemoryUsed += pool.getUsage().getUsed();
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
            
            return ResponseEntity.ok(new MemoryMetrics(
                heapUsage.getUsed() / 1024 / 1024,
                heapUsage.getMax() / 1024 / 1024,
                (double) heapUsage.getUsed() / heapUsage.getMax() * 100,
                nonHeapUsage.getUsed() / 1024 / 1024,
                directMemoryUsed / 1024 / 1024,
                Runtime.getRuntime().totalMemory() / 1024 / 1024,
                Runtime.getRuntime().freeMemory() / 1024 / 1024
            ));
        });
    }

    /**
     * Simple health check for load balancers.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<HealthResponse>> healthCheck() {
        return whisperService.getStatus()
                .map(status -> {
                    boolean isHealthy = status.health() != WhisperServiceImpl.HealthLevel.DEGRADED;

                    return ResponseEntity
                        .status(isHealthy ? 200 : 503)
                        .body(new HealthResponse(
                            isHealthy ? "UP" : "DOWN",
                            status.getHealthMessage()
                        ));
                });
    }

    /**
     * Force garbage collection (for debugging ONLY).
     * Use this to check if native memory is properly freed.
     */
    @SuppressWarnings("all") // => Suppress the bomb
    @PostMapping("/gc")
    public Mono<ResponseEntity<String>> forceGarbageCollection() {
        return Mono.fromCallable(() -> {
            long beforeHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            System.gc();
            System.runFinalization();
            Thread.sleep(1000);
            
            long afterHeap = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            long freedMB = (beforeHeap - afterHeap) / 1024 / 1024;
            
            return ResponseEntity.ok(String.format("GC completed. Freed: %d MB heap", freedMB));
        });
    }

    public record MemoryMetrics(
        long heapUsedMB,
        long heapMaxMB,
        double heapUsagePercent,
        long nonHeapUsedMB,
        long directMemoryUsedMB,  // Native memory (approximate)
        long totalMemoryMB,
        long freeMemoryMB
    ) {}

    public record HealthResponse(
        String status,
        String message
    ) {}
}