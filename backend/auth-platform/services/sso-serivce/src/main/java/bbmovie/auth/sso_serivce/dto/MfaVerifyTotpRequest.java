package bbmovie.auth.sso_serivce.dto;

public record MfaVerifyTotpRequest(String email, String totpCode) {
}
