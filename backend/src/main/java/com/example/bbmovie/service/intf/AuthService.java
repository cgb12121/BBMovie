package com.example.bbmovie.service.intf;

import com.example.bbmovie.dto.request.AuthRequest;
import com.example.bbmovie.dto.request.ChangePasswordRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.request.ResetPasswordRequest;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.dto.response.UserResponse;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;


public interface AuthService {
    AuthResponse register(RegisterRequest request);

    @Transactional
    void verifyEmail(String token);

    void sendVerificationEmail(String email);

    AuthResponse login(AuthRequest request);

    void logout(String accessToken);

    UserResponse loadAuthenticatedUser(String email);

    void changePassword(String requestEmail, @Valid ChangePasswordRequest request);

    void sendForgotPasswordEmail(String email);

    void resetPassword(String token, ResetPasswordRequest request);
}