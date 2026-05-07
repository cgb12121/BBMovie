package bbmovie.transcode.lgs.analysis;

import bbmovie.transcode.contracts.planning.ResolutionCostWeights;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps resolution labels to relative transcode cost weights for LGS planning.
 *
 * <p>Ported from transcode-worker {@code ResolutionCostCalculator}.</p>
 */
@Slf4j
public class LgsResolutionCostCalculator {

    /** Converts rendition label (e.g. 720p) to capacity cost weight. */
    public int calculateCost(String resolution) {
        int cost = ResolutionCostWeights.calculate(resolution);
        String normalized = resolution == null ? "" : resolution.toLowerCase().trim();
        if (cost == 6) {
            log.warn("Unknown resolution: {}, using default cost", normalized);
        }
        log.debug("Resolution {} -> cost weight: {}", normalized, cost);
        return cost;
    }
}
