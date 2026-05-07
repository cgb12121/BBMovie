package bbmovie.transcode.vis.probe;

import bbmovie.transcode.contracts.planning.ResolutionCostWeights;
import lombok.extern.slf4j.Slf4j;

/** Maps rendition labels to relative transcode cost weights for VIS planning. */
@Slf4j
public class VisResolutionCostCalculator {

    /** Converts resolution label (e.g. 720p) into capacity cost weight. */
    public int calculateCost(String resolution) {
        int cost = ResolutionCostWeights.calculate(resolution);
        String normalized = resolution == null ? "" : resolution.toLowerCase().trim();
        if (cost == 6) {
            log.warn("Unknown resolution: {}, using default cost", normalized);
        }
        return cost;
    }
}
