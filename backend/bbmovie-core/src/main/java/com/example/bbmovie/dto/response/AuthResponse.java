package com.example.bbmovie.dto.response;

import com.example.bbmovie.entity.enumerate.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;

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