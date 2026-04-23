package bbmovie.auth.mfa_service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VerifyMfaRequest(
        @NotBlank(message = "code is required")
        String code
) {
}
