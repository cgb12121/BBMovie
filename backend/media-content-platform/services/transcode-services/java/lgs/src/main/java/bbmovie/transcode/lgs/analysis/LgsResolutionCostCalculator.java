package bbmovie.transcode.lgs.analysis;

import lombok.extern.slf4j.Slf4j;

/**
 * Ported from transcode-worker {@code ResolutionCostCalculator}.
 */
@Slf4j
public class LgsResolutionCostCalculator {

    public int calculateCost(String resolution) {
        if (resolution == null || resolution.isEmpty()) {
            return 1;
        }
        String normalized = resolution.toLowerCase().trim();
        int cost = switch (normalized) {
            case "4320p", "2160p", "4k" -> 64;
            case "1080p" -> 32;
            case "720p" -> 16;
            case "480p" -> 8;
            case "360p" -> 4;
            case "240p" -> 2;
            case "144p" -> 1;
            case "original" -> 12;
            default -> {
                log.warn("Unknown resolution: {}, using default cost", normalized);
                yield 6;
            }
        };
        log.debug("Resolution {} -> cost weight: {}", normalized, cost);
        return cost;
    }
}
