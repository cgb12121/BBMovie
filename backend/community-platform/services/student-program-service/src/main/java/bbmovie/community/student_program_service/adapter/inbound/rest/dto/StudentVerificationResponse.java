package bbmovie.community.student_program_service.adapter.inbound.rest.dto;

import bbmovie.community.student_program_service.domain.StudentVerificationStatus;

public record StudentVerificationResponse(
        StudentVerificationStatus status,
        String documentUrl,
        String matchedUniversity,
        String message
) {
}
