package com.bbmovie.auth.security.jose.config;

@SuppressWarnings({"squid:S1118", "unused"})
public class JoseConstraint {
    public static class JoseHeader {
        public static final String TYPE = "type";
        public static final String ALGO = "alg";
    }

    public static class JosePayload {
        public static final String JTI = "jti";
        public static final String ROLE = "role";
        public static final String SID = "sid";
        public static final String SUB = "sub";
        public static final String EXP = "exp";
        public static final String IAT = "iat";
        public static final String ISS = "iss";

        public static class ABAC {
            public static final String SUBSCRIPTION_TIER = "subscriptionTier";
            public static final String AGE = "age";
            public static final String REGION = "region";
            public static final String PARENTAL_CONTROLS_ENABLED = "parentalControlsEnabled";
        }
    }
}
