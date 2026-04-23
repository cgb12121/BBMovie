package bbmovie.auth.identity_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendVerificationEmailRequest(@NotBlank @Email String email) {
}
