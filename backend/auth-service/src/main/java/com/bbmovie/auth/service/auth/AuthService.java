package com.bbmovie.auth.service.auth;

import com.bbmovie.auth.dto.request.ChangePasswordRequest;
import com.bbmovie.auth.dto.request.LoginRequest;
import com.bbmovie.auth.dto.request.RegisterRequest;
import com.bbmovie.auth.dto.request.ResetPasswordRequest;
import com.bbmovie.auth.dto.response.LoggedInDeviceResponse;
import com.bbmovie.auth.dto.response.LoginResponse;
import com.bbmovie.auth.dto.response.UserAgentResponse;
import com.bbmovie.auth.dto.response.UserResponse;
import com.bbmovie.auth.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;


import java.util.List;

/**
 * Facade interface
 */
@SuppressWarnings("unused")
public interface AuthService {
    // SessionService methods
    LoginResponse login(LoginRequest loginRequest, HttpServletRequest request);
    LoginResponse getLoginResponseFromOAuth2Login(UserDetails userDetails, HttpServletRequest request);
    void logoutFromCurrentDevice(String accessToken);
    void logoutFromOneDevice(String accessToken, String targetSid);
    List<LoggedInDeviceResponse> getAllLoggedInDevices(String jwtToken, HttpServletRequest request);
    void revokeAuthCookies(HttpServletResponse response);
    void updateUserTokensOnAbacChange(String email);

    // RegistrationService methods
    void register(RegisterRequest request);
    String verifyAccountByEmail(String token);
    void sendVerificationEmail(String email);
    void sendOtp(User user);
    void verifyAccountByOtp(String otp);

    // PasswordService methods
    void changePassword(String requestEmail, ChangePasswordRequest request);
    void sendForgotPasswordEmail(String email);
    void resetPassword(String token, ResetPasswordRequest request);

    // UserService methods
    UserResponse loadAuthenticatedUserInformation(String email);
    UserAgentResponse getUserDeviceInformation(HttpServletRequest request);
}
