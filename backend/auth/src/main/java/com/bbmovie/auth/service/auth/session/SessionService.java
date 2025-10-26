package com.bbmovie.auth.service.auth.session;

import com.bbmovie.auth.dto.request.LoginRequest;
import com.bbmovie.auth.dto.response.LoggedInDeviceResponse;
import com.bbmovie.auth.dto.response.LoginResponse;
import com.bbmovie.auth.dto.response.UserAgentResponse;
import com.bbmovie.auth.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SessionService {
    LoginResponse login(LoginRequest loginRequest, HttpServletRequest request);

    LoginResponse getLoginResponseFromOAuth2Login(UserDetails userDetails, HttpServletRequest request);

    UserResponse loadAuthenticatedUserInformation(String email);

    List<LoggedInDeviceResponse> getAllLoggedInDevices(String accessToken, HttpServletRequest request);

    void revokeAuthCookies(HttpServletResponse response);

    @Transactional
    void logoutFromCurrentDevice(String accessToken);

    @Transactional
    void logoutFromOneDevice(String accessToken, String targetSid);

    void logoutFromAllDevices(String email);

    UserAgentResponse getUserDeviceInformation(HttpServletRequest request);

    //need to check again
    void updateUserTokensOnAbacChange(String email);
}
