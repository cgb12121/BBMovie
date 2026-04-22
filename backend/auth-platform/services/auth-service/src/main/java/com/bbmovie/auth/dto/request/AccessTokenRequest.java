package com.bbmovie.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AccessTokenRequest {
    @NotBlank(message = "Access token is required")
    private String oldAccessToken;
} 