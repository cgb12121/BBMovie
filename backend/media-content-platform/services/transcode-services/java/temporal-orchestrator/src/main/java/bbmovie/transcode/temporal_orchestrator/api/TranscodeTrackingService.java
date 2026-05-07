package bbmovie.transcode.temporal_orchestrator.api;

import bbmovie.transcode.temporal_orchestrator.config.TemporalProperties;
import bbmovie.transcode.temporal_orchestrator.dto.TranscodeTrackingResponse;
import bbmovie.transcode.temporal_orchestrator.dto.WorkflowTrackingSnapshot;
import bbmovie.transcode.temporal_orchestrator.workflow.VideoProcessingWorkflow;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.api.enums.v1.WorkflowExecutionStatus;
import io.temporal.api.workflow.v1.WorkflowExecutionInfo;
import io.temporal.api.workflowservice.v1.DescribeWorkflowExecutionRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import io.temporal.serviceclient.WorkflowServiceStubs;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TranscodeTrackingService {

    private final WorkflowClient workflowClient;
    private final WorkflowServiceStubs workflowServiceStubs;
    private final TemporalProperties temporalProperties;

    public TranscodeTrackingResponse getStatus(String uploadId) {
        String workflowId = workflowId(uploadId);
        WorkflowExecutionInfo info = describe(workflowId);
        if (info == null) {
            return new TranscodeTrackingResponse(
                    uploadId,
                    workflowId,
                    "",
                    "NOT_FOUND",
                    "UNKNOWN",
                    "NOT_FOUND",
                    "workflow not found",
                    0,
                    0,
                    0,
                    "",
                    "",
                    List.of(),
                    null,
                    null,
                    null,
                    List.of(),
                    0L,
                    0L,
                    0L
            );
        }

        WorkflowTrackingSnapshot snapshot = querySnapshot(workflowId);
        return new TranscodeTrackingResponse(
                uploadId,
                workflowId,
                info.getExecution().getRunId(),
                normalizeExecutionStatus(info.getStatus()),
                snapshot.workflowPhase(),
                snapshot.lifecycleStatus(),
                snapshot.message(),
                snapshot.plannedRungs(),
                snapshot.encodedRungs(),
                snapshot.validatedRungs(),
                snapshot.masterPlaylistPath(),
                snapshot.error(),
                snapshot.failedQualityRungs(),
                snapshot.minVmafMean(),
                snapshot.minVmafP10(),
                snapshot.minVmafWorstWindow(),
                snapshot.timeline(),
                toEpochMillis(info.getStartTime().getSeconds(), info.getStartTime().getNanos()),
                toEpochMillis(info.getCloseTime().getSeconds(), info.getCloseTime().getNanos()),
                snapshot.updatedAtEpochMillis()
        );
    }

    private WorkflowExecutionInfo describe(String workflowId) {
        DescribeWorkflowExecutionRequest request = DescribeWorkflowExecutionRequest.newBuilder()
                .setNamespace(temporalProperties.getNamespace())
                .setExecution(WorkflowExecution.newBuilder().setWorkflowId(workflowId).build())
                .build();
        try {
            return workflowServiceStubs.blockingStub().describeWorkflowExecution(request).getWorkflowExecutionInfo();
        } catch (StatusRuntimeException e) {
            if (Status.NOT_FOUND.getCode().equals(e.getStatus().getCode())) {
                return null;
            }
            throw e;
        }
    }

    private WorkflowTrackingSnapshot querySnapshot(String workflowId) {
        VideoProcessingWorkflow stub = workflowClient.newWorkflowStub(VideoProcessingWorkflow.class, workflowId);
        try {
            WorkflowTrackingSnapshot snapshot = stub.getTrackingSnapshot();
            if (snapshot == null) {
                return defaultSnapshot();
            }
            return snapshot;
        } catch (WorkflowNotFoundException e) {
            return defaultSnapshot();
        } catch (Exception e) {
            String fallbackMessage = e.getMessage() != null ? e.getMessage() : "query_failed";
            return new WorkflowTrackingSnapshot(
                    "",
                    "UNKNOWN",
                    "UNKNOWN",
                    "query failed: " + fallbackMessage,
                    0,
                    0,
                    0,
                    "",
                    "",
                    List.of(),
                    null,
                    null,
                    null,
                    List.of("query_failed"),
                    0L
            );
        }
    }

    private static WorkflowTrackingSnapshot defaultSnapshot() {
        return new WorkflowTrackingSnapshot(
                "",
                "UNKNOWN",
                "UNKNOWN",
                "tracking unavailable",
                0,
                0,
                0,
                "",
                "",
                List.of(),
                null,
                null,
                null,
                List.of(),
                0L
        );
    }

    private static String normalizeExecutionStatus(WorkflowExecutionStatus status) {
        return status == null ? "UNKNOWN" : status.name().replace("WORKFLOW_EXECUTION_STATUS_", "");
    }

    private static long toEpochMillis(long seconds, int nanos) {
        if (seconds <= 0 && nanos <= 0) {
            return 0L;
        }
        return seconds * 1000L + nanos / 1_000_000L;
    }

    private static String workflowId(String uploadId) {
        return "transcode-" + uploadId;
    }
}
