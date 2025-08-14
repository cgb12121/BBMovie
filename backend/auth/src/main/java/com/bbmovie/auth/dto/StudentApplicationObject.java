package com.bbmovie.auth.dto;

import com.bbmovie.auth.entity.StudentProfile;
import com.bbmovie.auth.entity.enumerate.StudentVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentApplicationObject {
	
	private UUID id;
	private String email;
	private String displayedUsername;
	private StudentVerificationStatus status;
	private LocalDateTime applyStudentStatusDate;
	private boolean student;
	private LocalDateTime studentStatusExpireAt;
	private String studentDocumentUrl;

	public static StudentApplicationObject from(StudentProfile sp) {
		return StudentApplicationObject.builder()
				.id(sp.getUser().getId())
				.email(sp.getUser().getEmail())
				.displayedUsername(sp.getUser().getDisplayedUsername())
				.status(sp.getStudentVerificationStatus())
				.applyStudentStatusDate(sp.getApplyStudentStatusDate())
				.student(sp.isStudent())
				.studentStatusExpireAt(sp.getStudentStatusExpireAt())
				.studentDocumentUrl(sp.getStudentDocumentUrl())
				.build();
	}
}