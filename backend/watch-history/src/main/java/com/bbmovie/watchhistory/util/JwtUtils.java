package com.bbmovie.watchhistory.util;

import org.springframework.security.oauth2.jwt.Jwt;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER;

public final class JwtUtils {

    private JwtUtils() {
    }

    public static String getSubject(Jwt jwt) {
        return jwt.getSubject();
    }

    public static String getUserTier(Jwt jwt) {
        String userTier = jwt.getClaimAsString(SUBSCRIPTION_TIER);
        if (userTier == null || userTier.isBlank()) {
            return "FREE";
        }
        return userTier;
    }
}
