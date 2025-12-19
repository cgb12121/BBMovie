package com.bbmovie.auth.security.spel;

import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.security.jose.provider.JoseProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@SuppressWarnings("unused")
@Component("studentApplicationSecurity")
public class StudentApplicationSecurity {

    private final UserRepository userRepository;
    private final JoseProvider joseProvider;

    @Autowired
    public StudentApplicationSecurity(UserRepository userRepository, JoseProvider joseProvider) {
        this.userRepository = userRepository;
        this.joseProvider = joseProvider;
    }

    public boolean isOwner(String authorizationHeader, UUID applicationId) {
        if (authorizationHeader == null || applicationId == null) return false;

        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7)
                : authorizationHeader;


        try {
            if (!joseProvider.validateToken(token)) return false;
            String email = joseProvider.getUsernameFromToken(token);
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