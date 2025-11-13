package com.bbmovie.auth.security.jose.dto;

/**
 * A record that encapsulates an access token and a refresh token.
 * <p>
 * The {@code TokenPair} object is designed to represent a pair of tokens
 * commonly used in authentication and authorization workflows.
 * The access token is typically a short-lived token used for accessing
 * resources, while the refresh token is used to get new access tokens
 * once they expire.
 * <p>
 * Instances of this class are immutable and can be used as value objects.
 * <p>
 * Fields:
 * <p> - {@code accessToken}: Represents the short-lived token used for resource access.
 * <p> - {@code refreshToken}: Represents the token used to refresh or renew the access token.
 * <p>
 * Use this class in scenarios that involve token-based authentication
 * or session management where a pair of tokens is required.
 */
public record TokenPair(String accessToken, String refreshToken) {
}