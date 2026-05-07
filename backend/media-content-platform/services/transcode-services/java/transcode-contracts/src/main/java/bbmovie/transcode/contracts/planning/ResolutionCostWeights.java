package bbmovie.transcode.contracts.planning;

/**
 * Shared resolution-to-cost mapping for transcode planning workloads.
 */
public final class ResolutionCostWeights {

    private ResolutionCostWeights() {
    }

    /**
     * Converts resolution label (e.g. {@code 720p}) to relative capacity weight.
     */
    public static int calculate(String resolution) {
        if (resolution == null || resolution.isEmpty()) {
            return 1;
        }
        String normalized = resolution.toLowerCase().trim();
        return switch (normalized) {
            case "4320p", "2160p", "4k" -> 64;
            case "1080p" -> 32;
            case "720p" -> 16;
            case "480p" -> 8;
            case "360p" -> 4;
            case "240p" -> 2;
            case "144p" -> 1;
            case "original" -> 12;
            default -> 6;
        };
    }
}

