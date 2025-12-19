package com.bbmovie.common.entity;

/**
 * Utility class containing constants related to JOSE (JavaScript Object Signing and Encryption)
 * and JWT (JSON Web Token) structures used across the authentication and authorization system.
 *
 * <p>This class defines standard keys used in JWT headers and payloads, as well as Redis key
 * prefixes for handling blacklists like logout and ABAC (Attribute-Based Access Control).</p>
 */
@SuppressWarnings({"squid:S1118", "unused"}) // Utility class, no need for constructor
public class JoseConstraint {

    /**
     * Prefix for Redis keys used to store JWTs that have been invalidated due to user logout.
     * <p>Example Redis key: <code>logout-blacklist:{sid}</code></p>
     */
    public static final String JWT_LOGOUT_BLACKLIST_PREFIX = "logout-blacklist:";

    /**
     * Prefix for Redis keys used to store JWTs that have been invalidated due to ABAC (Attribute-Based Access Control) changes.
     * <p>Example Redis key: <code>abac-blacklist:{sid}</code></p>
     */
    public static final String JWT_ABAC_BLACKLIST_PREFIX = "abac-blacklist:";

    public enum JwtType {
         JWS, JWE;

        public String type() {
            return name().toLowerCase();
        }

        public static JwtType getType(String tokenString) {
            String[] parts = tokenString.split("\\.");
            return switch (parts.length) {
                case 3 -> JWS;
                case 5 -> JWE;
                default -> throw new IllegalArgumentException("Invalid JWT format");
            };
        }

        public static String getPayload(String tokenString) {
            String[] parts = tokenString.split("\\.");
            return switch (parts.length) {
                case 3 -> parts[1]; // payload for JWS
                case 5 -> parts[3]; // payload for JWE (after encryption)
                default -> throw new IllegalArgumentException("Invalid JWT format");
            };
        }
    }

    /**
     * Constants representing standard JOSE header fields in JWT.
     */
    public static class JoseHeader {
        /**
         * Header field that identifies the type of the token (e.g., "JWT").
         */
        public static final String TYPE = "type";

        /**
         * Header field that specifies the signing algorithm (e.g., "RS256", "HS256").
         */
        public static final String ALGO = "alg";

        /**
         * The header field representing the key ID used to sign the token.
         */
        public static final String KID = "kid";

        /**
         * Header field for the JWK Set URL pointing to public keys for verification.
         */
        public static final String JKU = "jku";
    }

    /**
     * Constants representing standard JWT payload claims used in access and refresh tokens.
     */
    public static class JosePayload {

        /**
         * Unique identifier for the token (JWT ID).
         */
        public static final String JTI = "jti";

        /**
         * User's role (e.g., ADMIN, USER).
         */
        public static final String ROLE = "role";

        /**
         * Session ID used to identify a particular login session.
         *
         * <p>Refresh and access token will have the same sid.</p>
         */
        public static final String SID = "sid";

        /**
         * Subject (or username/id) of the authenticated user.
         */
        public static final String SUB = "sub";

        /**
         * Email of the authenticated user.
         */
        public static final String EMAIL = "email";

        /**
         * Expiration time of the token (in epoch seconds).
         */
        public static final String EXP = "exp";

        /**
         * Issued-at time (in epoch seconds).
         */
        public static final String IAT = "iat";

        /**
         * Issuer of the token (e.g., name of the auth service or system).
         */
        public static final String ISS = "iss";

        /**
         * Constants for ABAC (Attribute-Based Access Control) attributes.
         */
        public static class ABAC {
            /**
             * Tier of the user's subscription (e.g., FREE, PREMIUM).
             */
            public static final String SUBSCRIPTION_TIER = "subscriptionTier";

            /**
             * Age of the user for age-restricted content control.
             */
            public static final String AGE = "age";

            /**
             * Geographical region of the user, used for region-based access control.
             */
            public static final String REGION = "region";

            /**
             * Whether parental controls are enabled for this account.
             */
            public static final String PARENTAL_CONTROLS_ENABLED = "parentalControlsEnabled";

            /**
             * Indicates whether a user's account is enabled.
             */
            public static final String IS_ACCOUNTING_ENABLED = "isEnabled";
        }
    }
}
