package bbmovie.transcode.vis.probe;

import java.util.List;

/**
 * Normalized VIS probe output shared across decision and profile-building stages.
 *
 * <p>Ported from transcode-worker {@code ProbeResult} video branch fields.</p>
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
