package com.bbmovie.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "User login request")
public class LoginRequest {
    @Schema(description = "User's email address", example = "[email]")
    @NotBlank(message = "Email is required")
    private String email;

    @Schema(description = "User's password", example = "password123")
    @NotBlank(message = "Password is required")
    private String password;
}