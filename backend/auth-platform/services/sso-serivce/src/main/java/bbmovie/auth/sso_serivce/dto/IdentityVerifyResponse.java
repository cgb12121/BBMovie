package bbmovie.auth.sso_serivce.dto;

public record IdentityVerifyResponse(
        boolean success,
        String userId,
        String email,
        String role,
        boolean enabled,
        boolean isMfaEnabled
) {
}
