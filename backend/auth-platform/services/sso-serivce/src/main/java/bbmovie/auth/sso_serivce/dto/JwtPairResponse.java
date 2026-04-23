package bbmovie.auth.sso_serivce.dto;

public record JwtPairResponse(
        String accessToken,
        String refreshToken,
        String sid,
        String email,
        String role
) {
}
