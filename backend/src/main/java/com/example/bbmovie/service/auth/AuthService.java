package com.example.bbmovie.service.auth;

import com.example.bbmovie.dto.request.LoginRequest;
import com.example.bbmovie.dto.request.ChangePasswordRequest;
import com.example.bbmovie.dto.request.RegisterRequest;
import com.example.bbmovie.dto.request.ResetPasswordRequest;
import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.dto.response.LoginResponse;
import com.example.bbmovie.dto.response.UserResponse;
import com.example.bbmovie.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.transaction.annotation.Transactional;


public interface AuthService {
    AuthResponse register(RegisterRequest request);

    @Transactional
    void verifyAccountByEmail(String token);

    @Transactional
    void verifyAccountByOtp(String otp);

    void sendVerificationEmail(String email);

    void sendOtp(String email);

    LoginResponse login(LoginRequest loginRequest, HttpServletRequest request);

    @Transactional
    void revokeAccessTokenAndRefreshToken(String accessToken, String deviceId);

    @Transactional
    void revokeAllTokensByEmail(String email, String deviceId);

    UserResponse loadAuthenticatedUserInformation(String email);

    User loadAuthenticatedUser(String email);

    void changePassword(String requestEmail, @Valid ChangePasswordRequest request);

    void sendForgotPasswordEmail(String email);

    void resetPassword(String token, ResetPasswordRequest request);

    void revokeCookies(HttpServletResponse response);
}