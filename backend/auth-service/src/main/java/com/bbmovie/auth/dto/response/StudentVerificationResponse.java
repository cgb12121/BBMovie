package com.bbmovie.auth.dto.response;

import com.bbmovie.auth.entity.enumerate.StudentVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentVerificationResponse {
	private StudentVerificationStatus status;
	private String documentUrl;
	private String matchedUniversity;
	private String message;
}


