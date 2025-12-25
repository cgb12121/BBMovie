package com.bbmovie.transcodeworker.service.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Calculates cost weights for video resolutions based on transcoding complexity.
 * <p>
 * Cost weights follow exponential scaling (multiply by 2 for each resolution level):
 * - 144p: 1 point
 * - 240p: 2 points
 * - 360p: 4 points
 * - 480p: 8 points
 * - 720p: 16 points
 * - 1080p: 32 points
 * <p>
 * The actual threads allocated to FFmpeg will be clamped to maxCapacity:
 * - If cost > maxCapacity: threads = maxCapacity (a system will wait for available capacity)
 * - If cost <= maxCapacity: threads = cost (exact allocation)
 * <p>
 * This ensures:
 * - High-end systems (16+ cores) can handle high resolutions efficiently
 * - Budget VPS (2-4 cores) automatically scales down to available capacity
 * - System never overloads, tasks wait if cost exceeds available capacity
 */
@Slf4j
@Component
public class ResolutionCostCalculator {

    @Autowired
    public ResolutionCostCalculator(@Value("${spring.profiles.active:dev}") String activeProfile) {
        boolean isProduction = "prod".equals(activeProfile) || "production".equals(activeProfile);
        
        // Auto-detect total logical processors (same as TranscodeScheduler)
        // This avoids circular dependency and ensures consistency
        int totalCores = Runtime.getRuntime().availableProcessors();
        
        log.info("ResolutionCostCalculator initialized - Total cores: {}, Production: {}", totalCores, isProduction);
    }

    /**
     * Calculates the cost weight for a given resolution.
     * <p>
     * Cost weights follow exponential scaling (multiply by 2):
     * - 144p: 1 point
     * - 240p: 2 points
     * - 360p: 4 points
     * - 480p: 8 points
     * - 720p: 16 points
     * - 1080p: 32 points
     * <p>
     * Note: This returns the COST WEIGHT, not the actual threads.
     * The scheduler will clamp the actual threads based on maxCapacity.
     * 
     * @param resolution The resolution suffix (e.g., "1080p", "720p", "480p", "original")
     * @return Cost weight (1, 2, 4, 8, 16, 32, etc.)
     */
    public int calculateCost(String resolution) {
        if (resolution == null || resolution.isEmpty()) {
            return 1; // Default to a minimum (1 point)
        }

        String normalized = resolution.toLowerCase().trim();

        // Exponential scaling: multiply by 2 for each resolution level
        int cost = switch (normalized) {
            // 4 K/8K: 64 points (very high cost)
            case "4080p", "2160p", "4k" -> 64;
            
            // 1080p: 32 points
            case "1080p" -> 32;
            
            // 720p: 16 points
            case "720p" -> 16;
            
            // 480p: 8 points
            case "480p" -> 8;
            
            // 360p: 4 points
            case "360p" -> 4;
            
            // 240p: 2 points
            case "240p" -> 2;
            
            // 144p: 1 point (minimum)
            case "144p" -> 1;
            
            // Unknown resolution: Most video should be 720p, but we will put it to be 2^6/5
            case "original" -> 12;
            
            default -> {
                log.warn("Unknown resolution: {}, using default cost of median", normalized);
                yield 6;
            }
        };
        
        log.debug("Resolution {} -> cost weight: {}", normalized, cost);
        return cost;
    }

    /**
     * Calculates cost weight based on resolution height.
     * Alternative method for when only height is known.
     * 
     * @param height Resolution height in pixels
     * @return Cost weight (1, 2, 4, 8, 16, 32, etc.)
     */
    public int calculateCostByHeight(int height) {
        if (height >= 2160) {
            return 64; // 4K
        } else if (height >= 1080) {
            return 32; // 1080p
        } else if (height >= 720) {
            return 16; // 720p
        } else if (height >= 480) {
            return 8; // 480p
        } else if (height >= 360) {
            return 4; // 360p
        } else if (height >= 240) {
            return 2; // 240p
        } else {
            return 1; // 144p and below
        }
    }

    /**
     * Clamps a value between min and max.
     *
     * @param value The value to clamp
     * @param min Minimum value
     * @param max Maximum value
     * @return Clamped value
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}

