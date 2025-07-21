package com.bbmovie.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private UserResponse userResponse;
    private AuthResponse authResponse;
    private UserAgentResponse userAgentResponse;

    public static LoginResponse fromUserResponse(UserResponse userResponse) {
        return LoginResponse.builder()
                .userResponse(userResponse)
                .build();
    }

    public static LoginResponse fromAuthResponse(AuthResponse authResponse) {
        return LoginResponse.builder()
                .authResponse(authResponse)
                .build();
    }

    public static LoginResponse fromUserAgentResponse(UserAgentResponse userAgentResponse) {
        return LoginResponse.builder()
                .userAgentResponse(userAgentResponse)
                .build();
    }

    public static LoginResponse fromUserAndAuthResponse(UserResponse userResponse, AuthResponse authResponse) {
        return LoginResponse.builder()
                .userResponse(userResponse)
                .authResponse(authResponse)
                .build();
    }

    public static LoginResponse fromUserAndAuthAndUserAgentResponse(UserResponse userResponse, AuthResponse authResponse, UserAgentResponse userAgentResponse) {
        return LoginResponse.builder()
                .userAgentResponse(userAgentResponse)
                .userResponse(userResponse)
                .authResponse(authResponse)
                .build();
    }
}