package com.bbmovie.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login response containing user, auth, and device information")
public class LoginResponse {

    @Schema(description = "User information")
    private UserResponse userResponse;
    
    @Schema(description = "Authentication information")
    private AuthResponse authResponse;
    
    @Schema(description = "Device and browser information")
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