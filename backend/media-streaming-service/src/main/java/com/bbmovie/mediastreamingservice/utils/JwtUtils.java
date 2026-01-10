package com.bbmovie.mediastreamingservice.utils;

import org.springframework.security.oauth2.jwt.Jwt;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER;

public class JwtUtils {

    private JwtUtils() {
    }

    /**
     * Extracts the user subscription tier from JWT token.
     * Defaults to "FREE" if tier is not present or blank.
     *
     * @param jwt The JWT token
     * @return The user's subscription tier
     */
    public static String getUserTier(Jwt jwt) {
        String userTier = jwt.getClaimAsString(SUBSCRIPTION_TIER);
        if (userTier == null || userTier.isBlank()) {
            return "FREE"; // Default to FREE if tier is not present
        }
        return userTier;
    }
}
