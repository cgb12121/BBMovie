package com.bbmovie.auth.security.spel;

import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.security.jose.JoseProviderStrategy;
import com.bbmovie.auth.security.jose.JoseProviderStrategyContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("studentApplicationSecurity")
public class StudentApplicationSecurity {

    private final UserRepository userRepository;
    private final JoseProviderStrategyContext joseContext;

    @Autowired
    public StudentApplicationSecurity(UserRepository userRepository, JoseProviderStrategyContext joseContext) {
        this.userRepository = userRepository;
        this.joseContext = joseContext;
    }

    public boolean isOwner(String authorizationHeader, UUID applicationId) {
        if (authorizationHeader == null || applicationId == null) return false;

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;

        JoseProviderStrategy provider = joseContext.getActiveProvider();
        if (provider == null) return false;
        try {
            if (!provider.validateToken(token)) return false;
            String email = provider.getUsernameFromToken(token);
            if (email == null) return false;
            return userRepository
                    .findByEmail(email)
                    .map(u -> applicationId.equals(u.getStudentProfile().getId()))
                    .orElse(false);
        } catch (Exception ex) {
            return false;
        }
    }
}