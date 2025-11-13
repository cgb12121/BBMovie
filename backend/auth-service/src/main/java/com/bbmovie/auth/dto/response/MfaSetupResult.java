package com.bbmovie.auth.dto.response;

public record MfaSetupResult(String secret, String qrCode) {}
