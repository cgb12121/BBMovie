package com.example.bbmovie.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccessTokenRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
} 