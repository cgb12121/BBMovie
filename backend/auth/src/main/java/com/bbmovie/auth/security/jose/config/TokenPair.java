package com.bbmovie.auth.security.jose.config;

public record TokenPair(String accessToken, String refreshToken) {
}