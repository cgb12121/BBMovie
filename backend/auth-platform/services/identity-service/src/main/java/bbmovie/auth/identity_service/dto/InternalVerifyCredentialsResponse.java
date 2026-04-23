package bbmovie.auth.identity_service.dto;

public record InternalVerifyCredentialsResponse(
        String userId,
        String email,
        String role,
        boolean enabled,
        boolean mfaEnabled
) {
}
