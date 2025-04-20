package com.example.bbmovie.service.intf;

import com.example.bbmovie.dto.request.AuthRequest;
import com.example.bbmovie.dto.request.ChangePasswordRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.dto.response.UserResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    void sendResetPasswordEmail(String email);

    void resetPassword(String token, String newPassword);
}