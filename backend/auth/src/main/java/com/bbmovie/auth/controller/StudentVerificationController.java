package com.bbmovie.auth.controller;

import com.bbmovie.auth.dto.request.StudentVerificationRequest;
import com.bbmovie.auth.dto.response.StudentVerificationResponse;
import com.bbmovie.auth.service.student.StudentVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentVerificationController {

	private final StudentVerificationService studentVerificationService;

	@PreAuthorize("isAuthenticated()")
	@PostMapping(value = "/apply", consumes = {"multipart/form-data"})
    public ResponseEntity<StudentVerificationResponse> apply(
            @RequestHeader("Authorization") String authorization,
            Authentication authentication,
            @Valid @RequestPart("payload") StudentVerificationRequest request,
            @RequestPart("document") MultipartFile document
    ) {
        String email = authentication.getName();
        StudentVerificationResponse resp = studentVerificationService.apply(authorization, email, request, document);
		return ResponseEntity.ok(resp);
	}
}


