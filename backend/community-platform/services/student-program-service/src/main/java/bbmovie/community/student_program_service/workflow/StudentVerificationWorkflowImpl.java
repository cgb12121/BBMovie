package bbmovie.community.student_program_service.workflow;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class StudentVerificationWorkflowImpl implements StudentVerificationWorkflow {
    private final StudentVerificationActivities activities = Workflow.newActivityStub(
            StudentVerificationActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofMinutes(1))
                    .build()
    );

    @Override
    public void run(StudentVerificationWorkflowInput input) {
        activities.requestManualReview(input);
    }
}
