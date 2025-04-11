package com.example.bbmovie.service.intf;

import com.example.bbmovie.dto.request.AuthRequest;
import com.example.bbmovie.dto.request.AccessTokenRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(AuthRequest request);
    AuthResponse refreshToken(AccessTokenRequest request);
    void logout(AccessTokenRequest request);
} 