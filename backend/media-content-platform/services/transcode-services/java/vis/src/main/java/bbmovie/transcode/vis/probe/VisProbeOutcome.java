package bbmovie.transcode.vis.probe;

import java.util.List;

/**
 * Ported from transcode-worker {@code ProbeResult} (video branch fields used by VIS).
 */
public record VisProbeOutcome(
        int width,
        int height,
        double duration,
        String codec,
        List<String> targetResolutions,
        int peakCost,
        int totalCost
) {
}
