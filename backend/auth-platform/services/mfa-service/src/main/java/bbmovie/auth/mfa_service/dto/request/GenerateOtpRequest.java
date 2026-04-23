package bbmovie.auth.mfa_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GenerateOtpRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String email
) {
}
