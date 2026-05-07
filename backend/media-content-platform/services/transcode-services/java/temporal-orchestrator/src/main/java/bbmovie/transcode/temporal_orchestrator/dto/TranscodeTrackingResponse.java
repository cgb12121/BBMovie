package bbmovie.transcode.temporal_orchestrator.dto;

import java.util.List;

public record TranscodeTrackingResponse(
        String uploadId,
        String workflowId,
        String runId,
        String executionStatus,
        String workflowPhase,
        String lifecycleStatus,
        String message,
        int plannedRungs,
        int encodedRungs,
        int validatedRungs,
        String masterPlaylistPath,
        String error,
        List<String> failedQualityRungs,
        Double minVmafMean,
        Double minVmafP10,
        Double minVmafWorstWindow,
        List<String> timeline,
        long startedAtEpochMillis,
        long closedAtEpochMillis,
        long updatedAtEpochMillis
) {
}
