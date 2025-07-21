package com.bbmovie.auth.service.auth;

import com.bbmovie.auth.dto.request.ChangePasswordRequest;
import com.bbmovie.auth.dto.request.LoginRequest;
import com.bbmovie.auth.dto.request.RegisterRequest;
import com.bbmovie.auth.dto.request.ResetPasswordRequest;
import com.bbmovie.auth.dto.response.*;
import com.bbmovie.auth.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface AuthService {
    UserAgentResponse getUserDeviceInformation(HttpServletRequest request);

    LoginResponse getLoginResponseFromOAuth2Login(UserDetails userDetails, HttpServletRequest request);

    AuthResponse register(RegisterRequest request);

    @Transactional
    String verifyAccountByEmail(String token);

    void sendOtp(User user);

    @Transactional
    void verifyAccountByOtp(String otp);

    void sendVerificationEmail(String email);

    LoginResponse login(LoginRequest loginRequest, HttpServletRequest request);

    @Transactional
    void logoutFromAllDevices(String email);

    @Transactional
    void logoutFromCurrentDevice(String email, String deviceName);

    List<LoggedInDeviceResponse> getAllLoggedInDevices(String email, HttpServletRequest request);

    UserResponse loadAuthenticatedUserInformation(String email);

    void changePassword(String requestEmail, @Valid ChangePasswordRequest request);

    void sendForgotPasswordEmail(String email);

    void resetPassword(String token, ResetPasswordRequest request);

    void revokeCookies(HttpServletResponse response);

    void logoutFromOneDevice(String username, String deviceName);
}