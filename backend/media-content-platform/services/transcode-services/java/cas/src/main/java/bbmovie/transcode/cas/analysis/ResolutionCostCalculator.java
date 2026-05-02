package bbmovie.transcode.cas.analysis;

import lombok.extern.slf4j.Slf4j;

/**
 * Ported from transcode-worker {@code ResolutionCostCalculator} (cost table only; no Spring profile wiring).
 */
@Slf4j
public class ResolutionCostCalculator {

    public int calculateCost(String resolution) {
        if (resolution == null || resolution.isEmpty()) {
            return 1;
        }

        String normalized = resolution.toLowerCase().trim();

        int cost = switch (normalized) {
            case "4080p", "2160p", "4k" -> 64;
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

    public int calculateCostByHeight(int height) {
        if (height >= 2160) {
            return 64;
        }
        if (height >= 1080) {
            return 32;
        }
        if (height >= 720) {
            return 16;
        }
        if (height >= 480) {
            return 8;
        }
        if (height >= 360) {
            return 4;
        }
        if (height >= 240) {
            return 2;
        }
        return 1;
    }
}
