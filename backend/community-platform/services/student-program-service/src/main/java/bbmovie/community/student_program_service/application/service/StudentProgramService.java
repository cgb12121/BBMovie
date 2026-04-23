package bbmovie.community.student_program_service.application.service;

import bbmovie.community.student_program_service.adapter.inbound.rest.dto.StudentApplicationResponse;
import bbmovie.community.student_program_service.adapter.inbound.rest.dto.StudentVerificationRequest;
import bbmovie.community.student_program_service.adapter.inbound.rest.dto.StudentVerificationResponse;
import bbmovie.community.student_program_service.domain.StudentVerificationStatus;
import bbmovie.community.student_program_service.domain.VerificationOutcome;
import bbmovie.community.student_program_service.infrastructure.persistence.entity.StudentProfileEntity;
import bbmovie.community.student_program_service.infrastructure.persistence.repo.StudentProfileRepository;
import bbmovie.community.student_program_service.workflow.StudentVerificationOrchestration;
import bbmovie.community.student_program_service.workflow.StudentVerificationWorkflowInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentProgramService {
    private final StudentProfileRepository repository;
    private final StudentVerificationOrchestration orchestration;

    @Transactional
    public StudentVerificationResponse apply(String userId, StudentVerificationRequest request) {
        StudentProfileEntity profile = repository.findByUserId(userId).orElseGet(StudentProfileEntity::new);
        profile.setUserId(userId);
        profile.setApplyStudentStatusDate(Instant.now());
        profile.setStudentVerificationStatus(StudentVerificationStatus.PENDING);
        profile.setStudent(false);
        profile.setStudentDocumentUrl(request.documentUrl() == null ? "pending-upload" : request.documentUrl());
        profile.setUniversityName(request.universityName());
        profile.setUniversityEmail(request.universityEmail());
        profile.setGraduationYear(request.graduationYear());
        repository.save(profile);
        orchestration.start(new StudentVerificationWorkflowInput(
                profile.getId(),
                userId,
                request.fullName(),
                request.universityName(),
                request.universityDomain(),
                request.universityCountry(),
                request.graduationYear(),
                request.universityEmail(),
                profile.getStudentDocumentUrl()
        ));

        return new StudentVerificationResponse(
                StudentVerificationStatus.PENDING,
                profile.getStudentDocumentUrl(),
                null,
                "Your student verification application has been submitted."
        );
    }

    public StudentApplicationResponse getApplication(String applicationId) {
        StudentProfileEntity entity = repository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        return toResponse(entity);
    }

    public List<StudentApplicationResponse> findAllApplications() {
        return repository.findByApplyStudentStatusDateIsNotNullOrderByApplyStudentStatusDateDesc()
                .stream().map(this::toResponse).toList();
    }

    public List<StudentApplicationResponse> findPending() {
        return repository.findByStudentVerificationStatusOrderByApplyStudentStatusDateDesc(StudentVerificationStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    public List<StudentApplicationResponse> findRejected() {
        return repository.findByStudentVerificationStatusOrderByApplyStudentStatusDateDesc(StudentVerificationStatus.REJECTED)
                .stream().map(this::toResponse).toList();
    }

    public List<StudentApplicationResponse> findStudents() {
        return repository.findByStudentTrueOrderByStudentStatusExpireAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public StudentVerificationResponse manuallyValidate(String userId, boolean approve) {
        StudentProfileEntity profile = repository.findByUserId(userId).orElseGet(StudentProfileEntity::new);
        profile.setUserId(userId);
        if (approve) {
            profile.setStudentVerificationStatus(StudentVerificationStatus.VERIFIED);
            profile.setStudent(true);
            profile.setStudentStatusExpireAt(Instant.now().plusSeconds(365L * 24 * 3600));
            profile.setLastMessage("Approved by admin");
        } else {
            profile.setStudentVerificationStatus(StudentVerificationStatus.REJECTED);
            profile.setStudent(false);
            profile.setStudentStatusExpireAt(null);
            profile.setLastMessage("Rejected by admin");
        }
        repository.save(profile);
        return new StudentVerificationResponse(
                profile.getStudentVerificationStatus(),
                profile.getStudentDocumentUrl(),
                null,
                profile.getLastMessage()
        );
    }

    @Transactional
    public void finalizeVerification(String applicationId, String status, String message) {
        StudentProfileEntity profile = repository.findById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));
        VerificationOutcome outcome;
        try {
            outcome = VerificationOutcome.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid outcome: " + status);
        }

        switch (outcome) {
            case VERIFIED, AUTO_APPROVE -> {
                profile.setStudentVerificationStatus(StudentVerificationStatus.VERIFIED);
                profile.setStudent(true);
                profile.setStudentStatusExpireAt(Instant.now().plusSeconds(365L * 24 * 3600));
            }
            case REJECTED, AUTO_REJECT -> {
                profile.setStudentVerificationStatus(StudentVerificationStatus.REJECTED);
                profile.setStudent(false);
                profile.setStudentStatusExpireAt(null);
            }
            case NEEDS_REVIEW -> profile.setStudentVerificationStatus(StudentVerificationStatus.PENDING);
        }
        profile.setLastMessage(message);
        repository.save(profile);
    }

    private StudentApplicationResponse toResponse(StudentProfileEntity e) {
        return new StudentApplicationResponse(
                e.getId(),
                e.getUserId(),
                e.getStudentVerificationStatus(),
                e.isStudent(),
                e.getApplyStudentStatusDate(),
                e.getStudentStatusExpireAt(),
                e.getStudentDocumentUrl(),
                e.getLastMessage(),
                e.getUniversityName(),
                e.getUniversityEmail(),
                e.getGraduationYear()
        );
    }
}
