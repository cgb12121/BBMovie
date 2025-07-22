package com.bbmovie.auth.security.jose;

import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;

public interface JoseProviderStrategy {
    String generateAccessToken(Authentication authentication, String sid);

    String generateRefreshToken(Authentication authentication, String sid);

    boolean validateToken(String token);

    String getUsernameFromToken(String token);

    boolean isAccessTokenBlacklistedForEmailAndDevice(String username, String deviceName);

    List<String> getRolesFromToken(String token);

    Date getExpirationDateFromToken(String token);

    void removeBlacklistedAccessTokenOfEmailAndDevice(String email, String deviceName);

    void invalidateAccessTokenByEmailAndDevice(String email, String deviceName);
}
