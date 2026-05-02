package bbmovie.transcode.temporal_orchestrator.workflow;

import bbmovie.transcode.temporal_orchestrator.dto.TranscodeJobInput;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface VideoProcessingWorkflow {

    @WorkflowMethod
    void processUpload(TranscodeJobInput input);
}
