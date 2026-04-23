package bbmovie.community.student_program_service.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StudentVerificationActivitiesImpl implements StudentVerificationActivities {
    @Override
    public void requestManualReview(StudentVerificationWorkflowInput input) {
        log.info(
                "Temporal student verification requested: applicationId={}, userId={}, university={}",
                input.applicationId(),
                input.userId(),
                input.universityName()
        );
    }
}
