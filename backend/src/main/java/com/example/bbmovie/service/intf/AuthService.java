package com.example.bbmovie.service.intf;

import com.example.bbmovie.dto.request.AuthRequest;
import com.example.bbmovie.dto.request.AccessTokenRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.response.AuthResponse;
import org.springframework.transaction.annotation.Transactional;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    @Transactional
    void verifyEmail(String token);

    void sendVerificationEmail(String email);

    AuthResponse login(AuthRequest request);
    AuthResponse refreshToken(AccessTokenRequest request);
    void logout(AccessTokenRequest request);
} 