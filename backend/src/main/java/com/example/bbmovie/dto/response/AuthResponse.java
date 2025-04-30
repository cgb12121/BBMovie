package com.example.bbmovie.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    private String email;

    @Builder.Default
    private String role = "ROLE_USER";
}