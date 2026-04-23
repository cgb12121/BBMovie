package bbmovie.community.student_program_service.workflow;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "student-program.temporal", name = "enabled", havingValue = "true")
public class StudentVerificationWorkflowLauncher implements StudentVerificationOrchestration {
    private final TemporalProperties temporalProperties;
    private final io.temporal.client.WorkflowClient workflowClient;

    @Override
    public void start(StudentVerificationWorkflowInput input) {
        io.temporal.client.WorkflowOptions options = io.temporal.client.WorkflowOptions.newBuilder()
                .setTaskQueue(temporalProperties.getTaskQueue())
                .setWorkflowId("student-verification-" + input.applicationId())
                .build();

        StudentVerificationWorkflow workflow = workflowClient.newWorkflowStub(StudentVerificationWorkflow.class, options);
        io.temporal.client.WorkflowClient.start(workflow::run, input);
    }
}
