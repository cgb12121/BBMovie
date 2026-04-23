package bbmovie.community.student_program_service.adapter.inbound.rest;

import bbmovie.community.student_program_service.adapter.inbound.rest.dto.ApiResponse;
import bbmovie.community.student_program_service.adapter.inbound.rest.dto.StudentApplicationResponse;
import bbmovie.community.student_program_service.adapter.inbound.rest.dto.StudentVerificationRequest;
import bbmovie.community.student_program_service.adapter.inbound.rest.dto.StudentVerificationResponse;
import bbmovie.community.student_program_service.application.service.StudentProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student-program")
public class StudentProgramController {
    private final StudentProgramService service;

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<StudentVerificationResponse> apply(
            @Valid @RequestBody StudentVerificationRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.success(service.apply(extractUserId(authentication), request));
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<StudentApplicationResponse> getById(@PathVariable String applicationId) {
        return ApiResponse.success(service.getApplication(applicationId));
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<StudentApplicationResponse>> findAllApplications() {
        return ApiResponse.success(service.findAllApplications());
    }

    @GetMapping("/applications/pending")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<StudentApplicationResponse>> findPending() {
        return ApiResponse.success(service.findPending());
    }

    @GetMapping("/applications/rejected")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<StudentApplicationResponse>> findRejected() {
        return ApiResponse.success(service.findRejected());
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<List<StudentApplicationResponse>> findStudents() {
        return ApiResponse.success(service.findStudents());
    }

    @PostMapping("/applications/{userId}/decision")
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public ApiResponse<StudentVerificationResponse> manuallyValidate(
            @PathVariable String userId,
            @RequestParam("approve") boolean approve
    ) {
        return ApiResponse.success(service.manuallyValidate(userId, approve));
    }

    @PostMapping("/internal/applications/{applicationId}/finalize")
    public ApiResponse<Void> finalizeVerification(
            @PathVariable String applicationId,
            @RequestParam("status") String status,
            @RequestParam(value = "message", required = false) String message
    ) {
        service.finalizeVerification(applicationId, status, message);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> me(JwtAuthenticationToken authentication) {
        return ApiResponse.success(extractUserId(authentication));
    }

    private String extractUserId(JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        String userId = jwt.getClaimAsString("user_id");
        if (userId == null || userId.isBlank()) {
            userId = jwt.getSubject();
        }
        return userId;
    }
}
