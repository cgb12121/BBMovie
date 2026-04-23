package bbmovie.auth.mfa_service.dto.response;

public record MfaSetupResponse(
        boolean success,
        String secret,
        String qrCode
) {
}
