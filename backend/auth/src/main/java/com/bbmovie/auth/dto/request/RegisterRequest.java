package com.bbmovie.auth.dto.request;

import com.bbmovie.auth.entity.enumerate.Region;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @NotBlank(message = "Phone number is required")
    @Size(min = 8, max = 15, message = "Phone number must be between 10 and 15 characters")
    private String phoneNumber;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotNull(message = "Age is required")
    @Max(value = 150, message = "Age must be between 1 and 150")
    @Min(value = 1, message = "Age must be between 1 and 150")
    private int age;

    @NotNull(message = "Region is required")
    private Region region;
}