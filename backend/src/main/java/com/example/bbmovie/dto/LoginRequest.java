package com.example.bbmovie.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
class LoginRequest {
    private String usernameOrEmail;
    private String password;

}
