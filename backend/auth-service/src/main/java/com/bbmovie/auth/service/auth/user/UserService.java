package com.bbmovie.auth.service.auth.user;

import com.bbmovie.auth.dto.response.UserAgentResponse;
import com.bbmovie.auth.dto.response.UserResponse;
import com.bbmovie.auth.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface UserService {
    UserResponse loadAuthenticatedUserInformation(String email);

    UserAgentResponse getUserDeviceInformation(HttpServletRequest request);

    Optional<User> findByEmail(String email);

    User createUserFromOAuth2(User user);
}
