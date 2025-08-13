package com.bbmovie.auth.service.student;

import com.bbmovie.auth.dto.request.StudentVerificationRequest;
import com.bbmovie.auth.dto.response.StudentVerificationResponse;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.UserNotFoundException;
import com.bbmovie.auth.repository.UserRepository;
import com.example.common.dtos.kafka.UploadMetadata;
import com.example.common.enums.EntityType;
import com.example.common.enums.Storage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Objects;
import java.util.Optional;

import static com.bbmovie.auth.entity.enumerate.StudentVerificationStatus.PENDING;
import static com.bbmovie.auth.entity.enumerate.StudentVerificationStatus.VERIFIED;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentVerificationService {

	private final UserRepository userRepository;
	private final UniversityRegistry universityRegistry;
	private final OcrExtractionService ocrExtractionService;
    private final ObjectMapper objectMapper;

	@Value("${services.file-service.base-url:http://localhost:8084}")
	private String fileServiceBaseUrl;

	@Transactional
    public StudentVerificationResponse apply(String bearerToken, String email, StudentVerificationRequest request, MultipartFile document) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UserNotFoundException("User not found"));

		validateDocument(document);

		Optional<String> extractedTextOpt = ocrExtractionService.extractText(document);
		Optional<String> matchedUniversity = Optional.empty();
		if (extractedTextOpt.isPresent()) {
			String text = extractedTextOpt.get().toLowerCase();
			matchedUniversity = universityRegistry.bestMatchByName(text);
		}

        boolean acceptedByAutomation = matchedUniversity
                .map(name -> textContains(request.getUniversityName(), name))
                .orElse(false);
        if (!acceptedByAutomation && universityRegistry.containsDomain(request.getUniversityDomain())) acceptedByAutomation = true;

		String uploadedUrl = uploadToFileService(bearerToken, document);

		user.setStudentDocumentUrl(uploadedUrl);
		user.setStudentVerificationStatus(acceptedByAutomation ? VERIFIED : PENDING);
		if (acceptedByAutomation) {
			user.setStudent(true);
			user.setStudentStatusExpireAt(LocalDateTime.now().plusYears(1));
		}
		userRepository.save(user);

		return StudentVerificationResponse.builder()
				.status(user.getStudentVerificationStatus())
				.documentUrl(uploadedUrl)
				.matchedUniversity(matchedUniversity.orElse(null))
				.message(acceptedByAutomation ? "Auto-verified" : "Submitted for manual review")
				.build();
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
            UploadMetadata metadata = new UploadMetadata(EntityType.STUDENT_DOCUMENT, Storage.LOCAL, null);

            MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
            ByteArrayResource fileResource = new ByteArrayResource(document.getBytes()) {
                @Override
                public String getFilename() {
                    return StringUtils.hasText(document.getOriginalFilename()) ? document.getOriginalFilename() : "student-document";
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
            ResponseEntity<String> response = restTemplate.postForEntity(fileServiceBaseUrl + "/file/upload/v1", requestEntity, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Upload failed");
            }
            return response.getBody();
        } catch (Exception ex) {
            throw new IllegalStateException("Upload failed: " + ex.getMessage(), ex);
        }
	}
}


