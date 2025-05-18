package com.example.bbmovie.security.jwt;

import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;

public interface JwtProviderStrategy {
    String generateAccessToken(Authentication authentication);

    String generateRefreshToken(Authentication authentication);

    boolean validateToken(String token);

    String getUsernameFromToken(String token);

    boolean isAccessTokenBlacklistedForEmailAndDevice(String username, String deviceName);

    List<String> getRolesFromToken(String token);

    Date getExpirationDateFromToken(String token);

    void removeJwtBlockAccessTokenOfEmailAndDevice(String email, String deviceName);

    void invalidateAccessTokenByEmailAndDevice(String email, String deviceName);
}
