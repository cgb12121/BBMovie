package bbmovie.transcode.vis.probe;

import org.junit.jupiter.api.Test;

import bbmovie.transcode.vis.dto.VisProbeOutcome;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisProbeDecisionPolicyTest {

    @Test
    void shouldRequireDeepProbeWhenFastOutcomeLooksUnreliable() {
        VisProbeDecisionPolicy policy = new VisProbeDecisionPolicy(2, 480);
        VisProbeOutcome outcome = new VisProbeOutcome(320, 180, 0.6, "", java.util.List.of("240p"), 1, 1);

        VisProbeDecisionPolicy.ProbeDecision decision = policy.decide(outcome, "video.bin");

        assertTrue(decision.deepProbeRequired());
        assertTrue(decision.confidence() < 0.9);
        assertFalse(decision.gateReasons().isEmpty());
        assertFalse(decision.riskFlags().isEmpty());
    }

    @Test
    void shouldSkipDeepProbeWhenFastOutcomeLooksHealthy() {
        VisProbeDecisionPolicy policy = new VisProbeDecisionPolicy(1, 240);
        VisProbeOutcome outcome = new VisProbeOutcome(1920, 1080, 120.0, "h264", java.util.List.of("1080p"), 5, 12);

        VisProbeDecisionPolicy.ProbeDecision decision = policy.decide(outcome, "video.mp4");

        assertFalse(decision.deepProbeRequired());
        assertTrue(decision.confidence() > 0.9);
    }

    @Test
    void shouldFlagBitrateOutlierForHighCostEstimates() {
        VisProbeDecisionPolicy policy = new VisProbeDecisionPolicy(1, 240);
        VisProbeOutcome outcome = new VisProbeOutcome(3840, 2160, 30.0, "h264", java.util.List.of("2160p"), 64, 120);
        VisProbeDecisionPolicy.ProbeDecision decision = policy.decide(outcome, "video.mp4");
        assertTrue(decision.riskFlags().contains("bitrate_outlier"));
    }
}
