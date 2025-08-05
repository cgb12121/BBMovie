package com.bbmovie.auth.security.jose;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.security.jose.config.TokenPair;
import com.example.common.annotation.Experimental;
import org.springframework.security.core.Authentication;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * An interface that defines a strategy for working with JOSE (JavaScript Object Signing and Encryption).
 * Provides methods for generating, validating, and decoding JOSE Tokens, as well as handling token
 * blacklists and extracting token claims.
 */
public interface JoseProviderStrategy {

    @Experimental
    TokenPair generateTokenPair(Authentication authentication, User loggedInUser);

    String generateAccessToken(Authentication authentication, String sid, User logInUser);

    String generateRefreshToken(Authentication authentication, String sid, User logInUser);

    boolean validateToken(String token);

    String getUsernameFromToken(String token);

    List<String> getRolesFromToken(String token);

    Date getExpirationDateFromToken(String token);

    String getJtiFromToken(String token);

    String getSidFromToken(String token);

    Map<String,Object> getClaimsFromToken(String token);

    Map<String,Object> getOnlyABACClaimsFromToken(String token);

    Map<String, Object> getOnlyABACFromClaims(Map<String, Object> tokenClaims);

    Date getIssuedAtFromToken(String token);

    boolean isTokenInLogoutBlacklist(String sid);

    void addTokenToLogoutBlacklist(String sid);

    void removeFromLogoutBlacklist(String sid);

    boolean isTokenInABACBlacklist(String sid);

    void addTokenToABACBlacklist(String sid);

    void removeTokenFromABACBlacklist(String sid);
}
