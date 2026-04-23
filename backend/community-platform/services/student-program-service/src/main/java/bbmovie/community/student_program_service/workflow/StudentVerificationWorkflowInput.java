package bbmovie.community.student_program_service.workflow;

public record StudentVerificationWorkflowInput(
        String applicationId,
        String userId,
        String fullName,
        String universityName,
        String universityDomain,
        String universityCountry,
        Integer graduationYear,
        String universityEmail,
        String documentUrl
) {
}
