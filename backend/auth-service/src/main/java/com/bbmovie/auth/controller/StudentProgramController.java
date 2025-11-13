package com.bbmovie.auth.controller;

import com.bbmovie.auth.dto.ApiResponse;
import com.bbmovie.auth.dto.StudentApplicationObject;
import com.bbmovie.auth.dto.response.CountryUniversityResponse;
import com.bbmovie.auth.dto.response.UniversityLookupResponse;
import com.bbmovie.auth.dto.request.StudentVerificationRequest;
import com.bbmovie.auth.dto.response.StudentVerificationResponse;
import com.bbmovie.auth.service.student.StudentVerificationService;
import com.bbmovie.auth.service.student.UniversityRegistryService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.parameters.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student-program")
public class StudentProgramController {

	private final StudentVerificationService studentVerificationService;
    private final UniversityRegistryService universityRegistryService;

    @Autowired
    public StudentProgramController(
            StudentVerificationService studentVerificationService,
            UniversityRegistryService universityRegistryService
    ) {
        this.studentVerificationService = studentVerificationService;
        this.universityRegistryService = universityRegistryService;
    }

    @PreAuthorize("isAuthenticated()")
	@PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<StudentVerificationResponse>> apply(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestPart("payload") StudentVerificationRequest request,
            @RequestPart("document") MultipartFile document,
            Authentication authentication
    ) {
        String email = authentication.getName();
        StudentVerificationResponse resp = studentVerificationService.apply(authorization, email, request, document);
		return ResponseEntity.ok(ApiResponse.success(resp));
	}

    @GetMapping("/supported")
    public ResponseEntity<ApiResponse<UniversityLookupResponse>> isUniversitySupported(@RequestParam("query") String query) {
        UniversityLookupResponse result = universityRegistryService.findByDomain(query);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/supported/countries")
    public ResponseEntity<ApiResponse<CountryUniversityResponse>> getUniversitiesByCountry(
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        if(null == country && code == null)
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid country or code"));
        CountryUniversityResponse response = universityRegistryService
                .getAllSupportedUniByCountryAndCode(country, code, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/applications/{applicationId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPER_ADMIN') " +
            "or @studentApplicationSecurity.isOwner(#authorization, #applicationId)"
    )
    public ResponseEntity<ApiResponse<StudentApplicationObject>> findApplicationById(
            @SuppressWarnings("unused") @P("authorization") @RequestHeader("Authorization") String authorization,
            @PathVariable UUID applicationId
    ) {
        return ResponseEntity.ok(ApiResponse.success(studentVerificationService.get(applicationId)));
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<java.util.List<StudentApplicationObject>>> findAllApplications() {
        return ResponseEntity.ok(ApiResponse.success(studentVerificationService.findAllApplication()));
    }

    @GetMapping("/applications/pending")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentApplicationObject>>> findAllPending() {
        return ResponseEntity.ok(ApiResponse.success(studentVerificationService.findAllPending()));
    }

    @GetMapping("/applications/rejected")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentApplicationObject>>> findAllRejected() {
        return ResponseEntity.ok(ApiResponse.success(studentVerificationService.findAllRejected()));
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<StudentApplicationObject>>> findAllStudents() {
        return ResponseEntity.ok(ApiResponse.success(studentVerificationService.findAllAccountWithStudentStatus()));
    }

    @PostMapping("/applications/{userId}/decision")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<StudentVerificationResponse>> manuallyValidate(
            @PathVariable UUID userId,
            @RequestParam("approve") boolean approve
    ) {
        return ResponseEntity.ok(ApiResponse.success(studentVerificationService.manuallyValidate(userId, approve)));
    }
}
