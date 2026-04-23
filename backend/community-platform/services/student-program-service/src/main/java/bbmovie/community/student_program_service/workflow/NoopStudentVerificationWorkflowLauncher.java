package bbmovie.community.student_program_service.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "student-program.temporal", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopStudentVerificationWorkflowLauncher implements StudentVerificationOrchestration {
    @Override
    public void start(StudentVerificationWorkflowInput input) {
        log.debug(
                "Temporal disabled, skip workflow launch for applicationId={}",
                input.applicationId()
        );
    }
}
