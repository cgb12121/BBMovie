package bbmovie.transcode.contracts.dto;

import java.io.Serializable;
import java.util.List;

/**
 * VIS inspection decision report describing confidence, risk flags, and estimated encode plan.
 *
 * @param probeMode probe path selected by VIS ({@code fast} or {@code deep})
 * @param confidence confidence score in [0, 1]
 * @param riskFlags deterministic advisory risk flags (estimate-only, non-blocking)
 * @param estimateOnly whether this report is advisory rather than authoritative
 * @param estimatePolicyVersion policy/version marker for VIS decision logic
 * @param gateReasons probe gate reasons collected during decision flow
 * @param selectedRungs estimated target rendition suffixes
 * @param peakCost estimated max per-rung cost
 * @param totalCost estimated summed cost
 * @param decisionTimeline compact event timeline for explainability
 */
public record VisDecisionReportDTO(
        String probeMode,
        double confidence,
        List<String> riskFlags,
        boolean estimateOnly,
        String estimatePolicyVersion,
        List<String> gateReasons,
        List<String> selectedRungs,
        int peakCost,
        int totalCost,
        List<String> decisionTimeline
) implements Serializable {
}

