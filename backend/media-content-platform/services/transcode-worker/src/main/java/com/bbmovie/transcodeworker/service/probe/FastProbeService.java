package com.bbmovie.transcodeworker.service.probe;

import com.bbmovie.transcodeworker.service.pipeline.dto.ProbeResult;
import com.bbmovie.transcodeworker.service.probe.strategy.ProbeStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Service for fast video metadata probing.
 * <p>
 * Uses strategy pattern to support multiple probing methods:
 * 1. Presigned URL (preferred) - FFprobe reads directly from URL
 * 2. Partial Download (fallback) - Download first N MB for probing
 * 3. Stream Pipe (alternative) - Pipe stream to FFprobe stdin
 * <p>
 * Strategies are tried in order of priority until one succeeds.
 */
@Slf4j
@Service
public class FastProbeService {

    private final List<ProbeStrategy> strategies;

    /**
     * Timeout for probing operations in seconds.
     */
    @Value("${app.probe.timeout-seconds:30}")
    private int probeTimeoutSeconds;

    /**
     * Creates FastProbeService with available strategies.
     * Strategies are sorted by priority (highest first).
     *
     * @param strategies List of available probe strategies (injected by Spring)
     */
    public FastProbeService(List<ProbeStrategy> strategies) {
        this.strategies = strategies.stream()
                .sorted(Comparator.comparingInt(ProbeStrategy::getPriority).reversed())
                .toList();
    }

    @PostConstruct
    public void init() {
        log.info("FastProbeService initialized with {} strategies: {}",
                strategies.size(),
                strategies.stream().map(ProbeStrategy::getName).toList());
    }

    /**
     * Probes a video file to extract metadata.
     * <p>
     * Tries each strategy in priority order until one succeeds.
     * Falls through to next strategy on failure.
     *
     * @param bucket MinIO bucket containing the file
     * @param key    Object key (file path)
     * @return ProbeResult with video metadata and cost calculations
     * @throws ProbeStrategy.ProbeException if all strategies fail
     */
    public ProbeResult probe(String bucket, String key) {
        log.debug("Starting probe for {}/{}", bucket, key);

        Exception lastException = null;

        for (ProbeStrategy strategy : strategies) {
            if (!strategy.supports(bucket, key)) {
                log.debug("Strategy {} does not support {}/{}", strategy.getName(), bucket, key);
                continue;
            }

            try {
                log.debug("Trying strategy {} for {}/{}", strategy.getName(), bucket, key);
                ProbeResult result = strategy.probe(bucket, key);
                log.info("Successfully probed {}/{} using {} strategy", bucket, key, strategy.getName());
                return result;

            } catch (Exception e) {
                log.warn("Strategy {} failed for {}/{}: {}", strategy.getName(), bucket, key, e.getMessage());
                lastException = e;
            }
        }

        String errorMsg = String.format("All probe strategies failed for %s/%s", bucket, key);
        log.error(errorMsg);
        throw new ProbeStrategy.ProbeException(errorMsg, lastException);
    }

    /**
     * Checks if probing is supported for the given file.
     *
     * @param bucket MinIO bucket
     * @param key    Object key
     * @return true if at least one strategy supports this file
     */
    public boolean supportsProbing(String bucket, String key) {
        return strategies.stream().anyMatch(s -> s.supports(bucket, key));
    }

    /**
     * Returns the list of available strategies for debugging.
     *
     * @return List of strategy names in priority order
     */
    public List<String> getAvailableStrategies() {
        return strategies.stream().map(ProbeStrategy::getName).toList();
    }
}

