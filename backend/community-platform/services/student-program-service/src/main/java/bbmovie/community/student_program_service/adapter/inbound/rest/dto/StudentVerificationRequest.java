package bbmovie.community.student_program_service.adapter.inbound.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudentVerificationRequest(
        @NotBlank(message = "studentId is required") String studentId,
        @NotBlank(message = "fullName is required") String fullName,
        @NotBlank(message = "universityName is required") String universityName,
        @NotBlank(message = "universityDomain is required") String universityDomain,
        @NotBlank(message = "universityCountry is required") String universityCountry,
        @NotNull(message = "graduationYear is required") Integer graduationYear,
        @Email String universityEmail,
        String documentUrl
) {
}
