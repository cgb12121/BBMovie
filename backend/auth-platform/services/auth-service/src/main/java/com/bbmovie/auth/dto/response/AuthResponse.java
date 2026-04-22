package com.bbmovie.auth.dto.response;

import com.bbmovie.auth.entity.enumerate.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response containing tokens and user role")
public class AuthResponse {

    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @JsonIgnore
    @Schema(hidden = true)
    private String refreshToken;

    @Schema(description = "User's email address", example = "[email]")
    private String email;

    @Schema(description = "User's role", example = "USER")
    @Builder.Default
    private Role role = Role.USER;
}