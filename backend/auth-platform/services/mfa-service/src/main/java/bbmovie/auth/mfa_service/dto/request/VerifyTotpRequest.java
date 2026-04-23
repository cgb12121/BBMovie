package bbmovie.auth.mfa_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyTotpRequest(
        @NotBlank(message = "email is required")
        String email,
        @NotBlank(message = "totpCode is required")
        String totpCode
) {
}
