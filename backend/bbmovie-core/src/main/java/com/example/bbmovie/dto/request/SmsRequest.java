package com.example.bbmovie.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SmsRequest {
    @NotBlank(message = "Phone number cannot be empty")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String toPhoneNumber;

    @NotBlank(message = "Message cannot be empty")
    private String message;
}