package bbmovie.community.student_program_service.infrastructure.persistence.entity;

import bbmovie.community.student_program_service.domain.StudentVerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "student_profiles")
public class StudentProfileEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false, unique = true, length = 64)
    private String userId;

    @Column(name = "is_student", nullable = false)
    private boolean student = false;

    @Column(name = "apply_student_status_date")
    private Instant applyStudentStatusDate;

    @Column(name = "student_status_expire_at")
    private Instant studentStatusExpireAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_verification_status", nullable = false, length = 32)
    private StudentVerificationStatus studentVerificationStatus = StudentVerificationStatus.NOT_STUDENT;

    @Column(name = "student_document_url", columnDefinition = "TEXT")
    private String studentDocumentUrl;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "university_name")
    private String universityName;

    @Column(name = "university_email")
    private String universityEmail;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        setCreatedAt(now);
        setUpdatedAt(now);
    }

    @PreUpdate
    public void onUpdate() {
        setUpdatedAt(Instant.now());
    }
}
