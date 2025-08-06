package com.bbmovie.auth.dto.request;

import com.bbmovie.auth.entity.enumerate.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "User registration request")
public class RegisterRequest {
    @Schema(description = "User's email address", example = "[email]")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Schema(description = "User's password", example = "password123", minLength = 8)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @Schema(description = "Password confirmation", example = "password123")
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @Schema(description = "User's phone number", example = "[phone_number]", minLength = 8, maxLength = 15)
    @NotBlank(message = "Phone number is required")
    @Size(min = 8, max = 15, message = "Phone number must be between 10 and 15 characters")
    private String phoneNumber;

    @Schema(description = "Unique username", example = "[username]", minLength = 3, maxLength = 20)
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @Schema(description = "User's first name", example = "[name]", minLength = 2, maxLength = 50)
    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @Schema(description = "User's last name", example = "[name]", minLength = 2, maxLength = 50)
    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Schema(description = "User's age", example = "25", minimum = "1", maximum = "150")
    @NotNull(message = "Age is required")
    @Max(value = 150, message = "Age must be between 1 and 150")
    @Min(value = 1, message = "Age must be between 1 and 150")
    private int age;

    @Schema(description = "User's region", example = "US")
    @NotNull(message = "Region is required")
    private Region region;
}