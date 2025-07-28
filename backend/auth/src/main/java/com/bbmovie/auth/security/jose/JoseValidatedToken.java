package com.bbmovie.auth.security.jose;


import java.util.List;

record JoseValidatedToken(
        JoseProviderStrategy provider,
        String sid,
        String username,
        List<String> roles,
        boolean isEnabled
) {}
