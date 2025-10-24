package com.bbmovie.ai_assistant_service.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

@Slf4j
public class JwtUtils {

    private JwtUtils() {
    }

    public static Authentication anonymousAuth(String anonymousId) {
        log.info("Anonymous authentication with id: {}", anonymousId);
        return new AnonymousAuthenticationToken(anonymousId, "ANONYMOUS", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    }

    public static String extractUserTier(Authentication authentication) {
        Optional<String> userTierOptional = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("TIER_"))
                .map(authority -> authority.substring("TIER_".length()))
                .findFirst();
        return userTierOptional.orElse("ANONYMOUS");
    }
}
