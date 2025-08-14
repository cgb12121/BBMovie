package com.bbmovie.auth.controller;

import com.bbmovie.auth.dto.ApiResponse;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            Authentication authentication,
            @Valid @RequestPart("payload") StudentVerificationRequest request,
            @RequestPart("document") MultipartFile document
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
            @RequestParam("country") String country,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size
    ) {
        CountryUniversityResponse response = universityRegistryService.getAllSupportedUniByCountry(country, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
