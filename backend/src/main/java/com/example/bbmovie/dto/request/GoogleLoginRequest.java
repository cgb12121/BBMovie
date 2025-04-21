package com.example.bbmovie.dto.request;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GoogleLoginRequest {
    private String credential;
}