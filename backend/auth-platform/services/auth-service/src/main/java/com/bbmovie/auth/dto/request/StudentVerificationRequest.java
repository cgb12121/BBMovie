package com.bbmovie.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StudentVerificationRequest {

	@NotBlank(message = "studentId is required")
	private String studentId;

	@NotBlank(message = "fullName is required")
	private String fullName;

	@NotBlank(message = "universityName is required")
	private String universityName;

	@NotBlank(message = "universityDomain is required")
	private String universityDomain;

	@NotBlank(message = "universityCountry is required")
	private String universityCountry;

	@NotNull(message = "graduationYear is required")
	private Integer graduationYear;

	@Email
	private String universityEmail;
}


