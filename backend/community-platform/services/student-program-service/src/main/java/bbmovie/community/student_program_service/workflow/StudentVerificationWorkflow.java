package bbmovie.community.student_program_service.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface StudentVerificationWorkflow {
    @WorkflowMethod
    void run(StudentVerificationWorkflowInput input);
}
