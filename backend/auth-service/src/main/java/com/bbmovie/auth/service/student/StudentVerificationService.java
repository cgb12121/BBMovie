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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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

		// TODO: Re-enable student verification document upload using media-upload-service / MinIO.
        // For now, this feature is explicitly disabled to allow decommissioning of the legacy file-service.
        throw new StudentApplicationException(
                "Student verification with document upload is temporarily unavailable while we migrate storage. Please try again later."
        );
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

    public StudentApplicationObject get(UUID applicationId) {
        return StudentApplicationObject.from(studentProfileRepository.findById(applicationId)
                .orElseThrow(() -> new StudentApplicationException("Unable to find application")));
    }
}