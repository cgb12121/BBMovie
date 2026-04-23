package bbmovie.community.student_program_service.adapter.inbound.rest.dto;

import bbmovie.community.student_program_service.domain.StudentVerificationStatus;

import java.time.Instant;

public record StudentApplicationResponse(
        String applicationId,
        String userId,
        StudentVerificationStatus status,
        boolean isStudent,
        Instant applyDate,
        Instant studentStatusExpireAt,
        String documentUrl,
        String message,
        String universityName,
        String universityEmail,
        Integer graduationYear
) {
}
