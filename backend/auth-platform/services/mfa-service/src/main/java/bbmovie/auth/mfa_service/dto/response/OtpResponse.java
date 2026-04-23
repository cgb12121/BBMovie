package bbmovie.auth.mfa_service.dto.response;

public record OtpResponse(
        boolean success,
        String otp
) {
}
