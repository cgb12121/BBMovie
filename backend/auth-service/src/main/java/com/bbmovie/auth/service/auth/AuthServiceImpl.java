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
import com.bbmovie.auth.service.auth.password.PasswordService;
import com.bbmovie.auth.service.auth.registration.RegistrationService;
import com.bbmovie.auth.service.auth.session.SessionService;
import com.bbmovie.auth.service.auth.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SessionService sessionService;
    private final RegistrationService registrationService;
    private final PasswordService passwordService;
    private final UserService userService;

    @Override
    public LoginResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        return sessionService.login(loginRequest, request);
    }

    @Override
    public LoginResponse getLoginResponseFromOAuth2Login(UserDetails userDetails, HttpServletRequest request) {
        return sessionService.getLoginResponseFromOAuth2Login(userDetails, request);
    }

    @Override
    public void logoutFromCurrentDevice(String accessToken) {
        sessionService.logoutFromCurrentDevice(accessToken);
    }

    @Override
    public void logoutFromOneDevice(String accessToken, String targetSid) {
        sessionService.logoutFromOneDevice(accessToken, targetSid);
    }

    @Override
    public List<LoggedInDeviceResponse> getAllLoggedInDevices(String jwtToken, HttpServletRequest request) {
        return sessionService.getAllLoggedInDevices(jwtToken, request);
    }

    @Override
    public void revokeAuthCookies(HttpServletResponse response) {
        sessionService.revokeAuthCookies(response);
    }

    @Override
    public void updateUserTokensOnAbacChange(String email) {
        sessionService.updateUserTokensOnAbacChange(email);
    }

    @Override
    public void register(RegisterRequest request) {
        registrationService.register(request);
    }

    @Override
    public String verifyAccountByEmail(String token) {
        return registrationService.verifyAccountByEmail(token);
    }

    @Override
    public void sendVerificationEmail(String email) {
        registrationService.sendVerificationEmail(email);
    }

    @Override
    public void sendOtp(User user) {
        registrationService.sendOtp(user);
    }

    @Override
    public void verifyAccountByOtp(String otp) {
        registrationService.verifyAccountByOtp(otp);
    }

    @Override
    public void changePassword(String requestEmail, ChangePasswordRequest request) {
        passwordService.changePassword(requestEmail, request);
    }

    @Override
    public void sendForgotPasswordEmail(String email) {
        passwordService.sendForgotPasswordEmail(email);
    }

    @Override
    public void resetPassword(String token, ResetPasswordRequest request) {
        passwordService.resetPassword(token, request);
    }

    @Override
    public UserResponse loadAuthenticatedUserInformation(String email) {
        return userService.loadAuthenticatedUserInformation(email);
    }

    @Override
    public UserAgentResponse getUserDeviceInformation(HttpServletRequest request) {
        return userService.getUserDeviceInformation(request);
    }
}
