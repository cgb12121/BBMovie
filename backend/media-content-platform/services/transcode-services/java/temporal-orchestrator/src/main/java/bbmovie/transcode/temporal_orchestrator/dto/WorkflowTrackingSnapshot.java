package bbmovie.transcode.temporal_orchestrator.dto;

import java.io.Serializable;
import java.util.List;

public record WorkflowTrackingSnapshot(
        String uploadId,
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
        long updatedAtEpochMillis
) implements Serializable {
}
