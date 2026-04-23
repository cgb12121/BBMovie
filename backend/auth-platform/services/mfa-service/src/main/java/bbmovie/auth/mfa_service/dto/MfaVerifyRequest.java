package bbmovie.auth.mfa_service.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyRequest(@NotBlank String code) {
}
