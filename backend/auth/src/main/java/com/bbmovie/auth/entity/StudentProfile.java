package com.bbmovie.auth.entity;

import com.bbmovie.auth.entity.enumerate.StudentVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static com.bbmovie.auth.entity.enumerate.StudentVerificationStatus.NOT_STUDENT;

@Builder
@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@ToString(exclude = { "user" })
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Builder.Default
	@Column(name = "is_student", nullable = false)
	private boolean student = false;

	@Column(name = "apply_student_status_date")
	private LocalDateTime applyStudentStatusDate;

	@Column(name = "student_status_expire_at")
	private LocalDateTime studentStatusExpireAt;

	@Builder.Default
	@Column(name = "student_verification_status")
	@Enumerated(EnumType.STRING)
	private StudentVerificationStatus studentVerificationStatus = NOT_STUDENT;

	@Column(name = "student_document_url", columnDefinition = "TEXT")
	private String studentDocumentUrl;
}