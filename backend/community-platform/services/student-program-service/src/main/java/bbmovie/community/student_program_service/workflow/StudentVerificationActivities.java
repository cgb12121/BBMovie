package bbmovie.community.student_program_service.workflow;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface StudentVerificationActivities {
    void requestManualReview(StudentVerificationWorkflowInput input);
}
