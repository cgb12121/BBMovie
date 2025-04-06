package com.example.bbmovie.service.intf;

import com.example.bbmovie.dto.request.AuthRequest;
import com.example.bbmovie.dto.request.RefreshTokenRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(RefreshTokenRequest request);
} 