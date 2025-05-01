package com.example.bbmovie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private UserResponse userResponse;
    private AuthResponse authResponse;

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

    public static LoginResponse fromUserAndAuthResponse(UserResponse userResponse, AuthResponse authResponse) {
        return LoginResponse.builder()
                .userResponse(userResponse)
                .authResponse(authResponse)
                .build();
    }
}