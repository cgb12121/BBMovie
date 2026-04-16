package com.bbmovie.auth.service.student;

import com.bbmovie.auth.dto.StudentApplicationObject;
import com.bbmovie.auth.dto.request.StudentVerificationRequest;
import com.bbmovie.auth.dto.response.StudentVerificationResponse;
import com.bbmovie.auth.entity.StudentProfile;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.StudentApplicationException;
import com.bbmovie.auth.exception.UserNotFoundException;
import com.bbmovie.auth.repository.StudentProfileRepository;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.enums.VerificationOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

import static com.bbmovie.auth.entity.enumerate.StudentVerificationStatus.PENDING;
import static com.bbmovie.auth.entity.enumerate.StudentVerificationStatus.REJECTED;
import static com.bbmovie.auth.entity.enumerate.StudentVerificationStatus.VERIFIED;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentVerificationService {

	private final UserRepository userRepository;
	private final StudentProfileRepository studentProfileRepository;
    private final RestTemplate restTemplate;

    @Value("${student.verification.camunda-engine-url:}")
    private String camundaEngineUrl;

	//TODO: will manually notify user about application (reject or approved, pending)
	@Transactional
	public StudentVerificationResponse apply(String bearerToken, String email, StudentVerificationRequest request, MultipartFile document) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found"));

        validateDocument(document);

        StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
                .orElse(StudentProfile.builder()
                        .user(user)
                        .build());

        profile.setApplyStudentStatusDate(LocalDateTime.now());
        profile.setStudentVerificationStatus(PENDING);
        // In a real scenario, we would upload to MinIO and store the URL
        profile.setStudentDocumentUrl("pending-upload");
        studentProfileRepository.save(profile);

        // Trigger Camunda Workflow
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("applicationId", profile.getId().toString());
            variables.put("userId", user.getId().toString());
            variables.put("fullName", request.getFullName());
            variables.put("universityName", request.getUniversityName());
            variables.put("universityDomain", request.getUniversityDomain());
            variables.put("universityEmail", request.getUniversityEmail());
            variables.put("graduationYear", request.getGraduationYear());
            variables.put("documentName", document.getOriginalFilename());
            variables.put("documentContent", Base64.getEncoder().encodeToString(document.getBytes()));

            restTemplate.postForObject(camundaEngineUrl, variables, Map.class);
            log.info("Triggered student verification workflow for application {}", profile.getId());
        } catch (Exception e) {
            log.error("Failed to trigger verification workflow", e);
            // We still return PENDING as it's saved in DB and can be picked up later
        }

        return StudentVerificationResponse.builder()
                .status(PENDING)
                .message("Your student verification application has been submitted and is being processed.")
                .build();
	}

	public List<StudentApplicationObject> findAllApplication() {
		return studentProfileRepository
				.findByApplyStudentStatusDateIsNotNullOrderByApplyStudentStatusDateDesc()
				.stream()
				.map(StudentApplicationObject::from)
				.toList();
	}

	public List<StudentApplicationObject> findAllPending() {
		return studentProfileRepository
				.findByStudentVerificationStatusOrderByApplyStudentStatusDateDesc(PENDING)
				.stream()
				.map(StudentApplicationObject::from)
				.toList();
	}

	public List<StudentApplicationObject> findAllRejected() {
		return studentProfileRepository
				.findByStudentVerificationStatusOrderByApplyStudentStatusDateDesc(REJECTED)
				.stream()
				.map(StudentApplicationObject::from)
				.toList();
	}

	public List<StudentApplicationObject> findAllAccountWithStudentStatus() {
		return studentProfileRepository
				.findByStudentTrueOrderByStudentStatusExpireAtDesc()
				.stream()
				.map(StudentApplicationObject::from)
				.toList();
	}

	@Transactional
	public StudentVerificationResponse manuallyValidate(UUID userId, boolean approve) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		StudentProfile profile = studentProfileRepository.findByUserId(userId)
				.orElseGet(() ->
                        StudentProfile.builder()
						.user(user)
						.build()
                );

		if (approve) {
			profile.setStudentVerificationStatus(VERIFIED);
			profile.setStudent(true);
			profile.setStudentStatusExpireAt(LocalDateTime.now().plusYears(1));
			studentProfileRepository.save(profile);
			return StudentVerificationResponse.builder()
					.status(profile.getStudentVerificationStatus())
					.documentUrl(profile.getStudentDocumentUrl())
					.matchedUniversity(null)
					.message("Your application has been approved by admin.")
					.build();
		} else {
			profile.setStudentVerificationStatus(REJECTED);
			profile.setStudent(false);
			profile.setStudentStatusExpireAt(null);
			studentProfileRepository.save(profile);
			return StudentVerificationResponse.builder()
					.status(profile.getStudentVerificationStatus())
					.documentUrl(profile.getStudentDocumentUrl())
					.matchedUniversity(null)
					.message("Your application got rejected by admin.")
					.build();
		}
	}

    @Transactional
    public void finalizeVerification(UUID applicationId, String status, String message) {
        StudentProfile profile = studentProfileRepository.findById(applicationId)
                .orElseThrow(() -> new StudentApplicationException("Unable to find application"));

        VerificationOutcome outcome;
        try {
            outcome = VerificationOutcome.valueOf(status);
        } catch (IllegalArgumentException e) {
            log.error("Invalid verification outcome received: {}", status);
            return;
        }

        switch (outcome) {
            case VERIFIED:
            case AUTO_APPROVE:
                profile.setStudentVerificationStatus(VERIFIED);
                profile.setStudent(true);
                profile.setStudentStatusExpireAt(LocalDateTime.now().plusYears(1));
                break;
            case REJECTED:
            case AUTO_REJECT:
                profile.setStudentVerificationStatus(REJECTED);
                profile.setStudent(false);
                break;
            case NEEDS_REVIEW:
                profile.setStudentVerificationStatus(PENDING);
                break;
        }

        studentProfileRepository.save(profile);
        log.info("Finalized verification for application {}: outcome={}, message={}", applicationId, outcome, message);
    }

	private void validateDocument(MultipartFile document) {
		String contentType = document.getContentType();
		if (contentType == null) throw new IllegalArgumentException("Unsupported file type");
		boolean allowed = contentType.equals(MediaType.APPLICATION_PDF_VALUE)
				|| contentType.startsWith(MediaType.IMAGE_PNG_VALUE)
				|| contentType.startsWith(MediaType.IMAGE_JPEG_VALUE)
				|| contentType.equals("image/jpg");
		if (!allowed) throw new IllegalArgumentException("Only PDF, PNG, JPG, JPEG are allowed");
	}

    public StudentApplicationObject get(UUID applicationId) {
        return StudentApplicationObject.from(studentProfileRepository.findById(applicationId)
                .orElseThrow(() -> new StudentApplicationException("Unable to find application")));
    }
}