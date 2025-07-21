package com.bbmovie.auth.dto.response;

import com.bbmovie.auth.entity.enumerate.Role;
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
    private Role role = Role.USER;
}