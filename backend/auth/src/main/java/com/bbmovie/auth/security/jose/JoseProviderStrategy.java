package com.bbmovie.auth.security.jose;

import com.bbmovie.auth.entity.User;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface JoseProviderStrategy {
    String generateAccessToken(Authentication authentication, String sid, User logInUser);

    String generateRefreshToken(Authentication authentication, String sid, User logInUser);

    boolean validateToken(String token);

    String getUsernameFromToken(String token);

    boolean isAccessTokenBlacklistedForEmailAndDevice(String username, String deviceName);

    List<String> getRolesFromToken(String token);

    Date getExpirationDateFromToken(String token);

    void removeBlacklistedAccessTokenOfEmailAndDevice(String email, String deviceName);

    void invalidateAccessTokenByEmailAndDevice(String email, String deviceName);

    String getJtiFromToken(String token);

    String getSidFromToken(String token);

    Map<String,Object> getClaimsFromToken(String token);

    Map<String,Object> getOnlyABACClaimsFromToken(String token);

    Map<String, Object> getOnlyABACFromClaims(Map<String, Object> tokenClaims);
}
