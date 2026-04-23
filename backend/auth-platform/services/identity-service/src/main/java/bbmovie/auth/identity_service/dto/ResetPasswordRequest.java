package bbmovie.auth.identity_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank @Size(min = 8) String newPassword,
        @NotBlank String confirmNewPassword
) {
}
