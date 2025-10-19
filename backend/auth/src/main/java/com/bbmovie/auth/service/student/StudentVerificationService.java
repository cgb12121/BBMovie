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
import com.bbmovie.auth.security.jose.provider.JoseProvider;
import com.example.common.dtos.nats.UploadMetadata;
import com.example.common.enums.EntityType;
import com.example.common.enums.Storage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static com.bbmovie.auth.entity.enumerate.StudentVerificationStatus.PENDING;
import static com.bbmovie.auth.entity.enumerate.StudentVerificationStatus.REJECTED;
import static com.bbmovie.auth.entity.enumerate.StudentVerificationStatus.VERIFIED;

@Slf4j
@Service
public class StudentVerificationService {

	private final UserRepository userRepository;
	private final StudentProfileRepository studentProfileRepository;
	private final UniversityRegistryService universityRegistryService;
	private final OcrExtractionService ocrExtractionService;
	private final ObjectMapper objectMapper;
    private final JoseProvider joseProvider;

    @Value("${services.file-service.base-url:http://localhost:8084}")
	private String fileServiceBaseUrl;

	@Autowired
	public StudentVerificationService(
			UserRepository userRepository,
			StudentProfileRepository studentProfileRepository,
			UniversityRegistryService universityRegistryService,
			OcrExtractionService ocrExtractionService,
            JoseProvider joseProvider
	) {
		this.userRepository = userRepository;
		this.studentProfileRepository = studentProfileRepository;
		this.universityRegistryService = universityRegistryService;
		this.ocrExtractionService = ocrExtractionService;
		this.joseProvider = joseProvider;
		this.objectMapper = new ObjectMapper();
	}

	//TODO: will manually notify user about application (reject or approved, pending)
	@Transactional
	public StudentVerificationResponse apply(String bearerToken, String email, StudentVerificationRequest request, MultipartFile document) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		validateDocument(document);

		Optional<String> extractedTextOpt = ocrExtractionService.extractText(document);
		Optional<String> matchedUniversity = Optional.empty();
		if (extractedTextOpt.isPresent()) {
			String text = extractedTextOpt.get().toLowerCase();
			matchedUniversity = universityRegistryService.bestMatchByName(text);
		}

		boolean acceptedByAutomation = matchedUniversity
				.map(name -> textContains(request.getUniversityName(), name))
				.orElse(false);
		if (!acceptedByAutomation && universityRegistryService.containsDomain(request.getUniversityDomain())) {
			acceptedByAutomation = true;
		}

		String uploadedUrl = uploadToFileService(bearerToken, document);

		StudentProfile profile = studentProfileRepository.findByUserId(user.getId())
				.orElseGet(() -> StudentProfile.builder()
						.user(user)
						.build());

		profile.setApplyStudentStatusDate(LocalDateTime.now());
		profile.setStudentDocumentUrl(uploadedUrl);
		profile.setStudentVerificationStatus(acceptedByAutomation ? VERIFIED : PENDING);
		if (acceptedByAutomation) {
			profile.setStudent(true);
			profile.setStudentStatusExpireAt(LocalDateTime.now().plusYears(1));
		}
		studentProfileRepository.save(profile);

		return StudentVerificationResponse.builder()
				.status(profile.getStudentVerificationStatus())
				.documentUrl(uploadedUrl)
				.matchedUniversity(matchedUniversity.orElse(null))
				.message(acceptedByAutomation ? "Auto-verified" : "Submitted for manual review")
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

	private boolean textContains(String requestValue, String matched) {
		if (!StringUtils.hasText(requestValue) || !StringUtils.hasText(matched)) return false;
		String req = requestValue.toLowerCase();
		String m = matched.toLowerCase();
		return req.contains(m) || m.contains(req);
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

	private String uploadToFileService(String bearerToken, MultipartFile document) {
		try {
			String jwt;
			if (bearerToken.startsWith("Bearer ")) {
				jwt = bearerToken.substring(7);
			} else {
				jwt = bearerToken;
			}
			UploadMetadata metadata = new UploadMetadata(UUID.randomUUID().toString() ,EntityType.STUDENT_DOCUMENT, Storage.LOCAL, null);

			MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
			ByteArrayResource fileResource = new ByteArrayResource(document.getBytes()) {
				@Override
				public String getFilename() {
					return joseProvider.getUsernameFromToken(jwt) + "_" + LocalDateTime.now();
				}
			};

			HttpHeaders fileHeaders = new HttpHeaders();
			fileHeaders.setContentType(MediaType.parseMediaType(Objects.requireNonNull(document.getContentType())));
			HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(fileResource, fileHeaders);

			HttpHeaders metaHeaders = new HttpHeaders();
			metaHeaders.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> metaEntity = new HttpEntity<>(objectMapper.writeValueAsString(metadata), metaHeaders);

			parts.add("file", fileEntity);
			parts.add("metadata", metaEntity);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			headers.set("Authorization", bearerToken);

			HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(parts, headers);
			RestTemplate restTemplate = new RestTemplate();
			ResponseEntity<String> response = restTemplate
					.postForEntity(fileServiceBaseUrl + "/file/upload/v1", requestEntity, String.class);
			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
				throw new IllegalStateException("Upload failed");
			}
			return response.getBody();
		} catch (Exception ex) {
			throw new IllegalStateException("Upload failed: " + ex.getMessage(), ex);
		}
	}

    public StudentApplicationObject get(UUID applicationId) {
        return StudentApplicationObject.from(studentProfileRepository.findById(applicationId)
                .orElseThrow(() -> new StudentApplicationException("Unable to find application")));
    }
}