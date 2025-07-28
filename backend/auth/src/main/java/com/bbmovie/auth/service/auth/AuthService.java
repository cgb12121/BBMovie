package com.bbmovie.auth.service.auth;

import com.bbmovie.auth.dto.request.ChangePasswordRequest;
import com.bbmovie.auth.dto.request.LoginRequest;
import com.bbmovie.auth.dto.request.RegisterRequest;
import com.bbmovie.auth.dto.request.ResetPasswordRequest;
import com.bbmovie.auth.dto.response.*;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.exception.CustomEmailException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


public interface AuthService {

    LoginResponse login(LoginRequest loginRequest, HttpServletRequest request);

    UserAgentResponse getUserDeviceInformation(HttpServletRequest request);

    LoginResponse getLoginResponseFromOAuth2Login(UserDetails userDetails, HttpServletRequest request);

    @Transactional
    AuthResponse register(RegisterRequest request);

    @Transactional
    String verifyAccountByEmail(String token);

    void sendVerificationEmail(String email);

    void sendOtp(User user);

    @Transactional
    void verifyAccountByOtp(String otp);

    @Transactional
    void logoutFromCurrentDevice(String accessToken);

    @Transactional
    void logoutFromOneDevice(String accessToken, String targetSid);

    List<LoggedInDeviceResponse> getAllLoggedInDevices(String email, HttpServletRequest request);

    //need check again
    @Transactional
    void updateUserTokensOnAbacChange(String email);

    UserResponse loadAuthenticatedUserInformation(String email);

    @Transactional(noRollbackFor = CustomEmailException.class)
    void changePassword(String requestEmail, ChangePasswordRequest request);

    void sendForgotPasswordEmail(String email);

    void resetPassword(String token, ResetPasswordRequest request);

    void revokeAuthCookies(HttpServletResponse response);
}