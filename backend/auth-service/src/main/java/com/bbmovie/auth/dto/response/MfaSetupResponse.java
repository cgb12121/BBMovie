package com.bbmovie.auth.dto.response;

public record MfaSetupResponse(String secret, String qrCode) {

}