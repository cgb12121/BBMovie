package bbmovie.transcode.vis.probe;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class VisProbeDecisionPolicy {

    private final int minDurationSecondsForTrust;
    private final int minWidthForTrust;

    public VisProbeDecisionPolicy(
            @Value("${vis.profile-v2.min-duration-seconds-for-trust:1}") int minDurationSecondsForTrust,
            @Value("${vis.profile-v2.min-width-for-trust:240}") int minWidthForTrust) {
        this.minDurationSecondsForTrust = Math.max(0, minDurationSecondsForTrust);
        this.minWidthForTrust = Math.max(0, minWidthForTrust);
    }

    public ProbeDecision decide(VisProbeOutcome fastOutcome, String key) {
        List<String> reasons = new ArrayList<>();
        if (fastOutcome.duration() < minDurationSecondsForTrust) {
            reasons.add("duration_too_small");
        }
        if (fastOutcome.width() < minWidthForTrust) {
            reasons.add("width_too_small");
        }
        if (fastOutcome.codec() == null || fastOutcome.codec().isBlank()) {
            reasons.add("codec_missing");
        }
        if (!looksLikeContainerKey(key)) {
            reasons.add("suspicious_extension");
        }
        boolean deepProbeRequired = !reasons.isEmpty();
        double confidence = deepProbeRequired ? 0.55 : 0.92;
        return new ProbeDecision(deepProbeRequired, confidence, reasons);
    }

    private static boolean looksLikeContainerKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase();
        return lower.endsWith(".mp4")
                || lower.endsWith(".mkv")
                || lower.endsWith(".mov")
                || lower.endsWith(".webm")
                || lower.endsWith(".avi")
                || lower.endsWith(".m4v");
    }

    public record ProbeDecision(boolean deepProbeRequired, double confidence, List<String> gateReasons) {
    }
}
