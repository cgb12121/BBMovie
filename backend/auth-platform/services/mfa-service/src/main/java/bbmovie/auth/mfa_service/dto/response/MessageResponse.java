package bbmovie.auth.mfa_service.dto.response;

public record MessageResponse(
        boolean success,
        String message
) {
}
