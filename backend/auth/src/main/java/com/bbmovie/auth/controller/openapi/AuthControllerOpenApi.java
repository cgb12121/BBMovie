package com.bbmovie.auth.controller.openapi;

import com.bbmovie.auth.dto.ApiResponse;
import com.bbmovie.auth.dto.request.*;
import com.bbmovie.auth.dto.response.AccessTokenResponse;
import com.bbmovie.auth.dto.response.AuthResponse;
import com.bbmovie.auth.dto.response.LoginResponse;
import com.bbmovie.auth.dto.response.UserAgentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@SuppressWarnings("unused")
@Tag(name = "Authentication", description = "Authentication and authorization API for BBMovie application")
public interface AuthControllerOpenApi {

    @Operation(
        summary = "Test endpoint",
        description = "Simple test endpoint to verify API connectivity"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Successful response",
        content = @Content(
            mediaType = "text/plain",
            schema = @Schema(type = "string"),
            examples = @ExampleObject(value = "Hello World")
        )
    )
    @Tag(name = "Test")
    ResponseEntity<String> test();

    @Operation(
        summary = "Register a new user",
        description = "Register a new user account with email verification"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Registration successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "Registration successful. Please check your email for verification."
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "success": false,
                        "message": "Validation failed",
                        "errors": {
                            "email": "Invalid email format",
                            "password": "Password must be at least 8 characters long"
                        }
                    }
                    """)
            )
        )
    })
    @Tag(name = "Authentication")
    ResponseEntity<ApiResponse<AuthResponse>> register(
        @Valid @RequestBody RegisterRequest request,
        BindingResult bindingResult
    );

    @Operation(
        summary = "User login",
        description = "Authenticate user and return access tokens"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Login successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @Tag(name = "Authentication")
    ResponseEntity<ApiResponse<LoginResponse>> login(
        @Valid @RequestBody LoginRequest loginRequest,
        HttpServletRequest request,
        BindingResult bindingResult
    );

    @Operation(
        summary = "Get new access token (Legacy)",
        description = "Legacy endpoint to refresh access token",
        deprecated = true
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "New access token generated",
            content = @Content(
                mediaType = "text/plain",
                schema = @Schema(type = "string", description = "New access token")
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad request - missing or invalid token"
        )
    })
    @Tag(name = "Token Management")
    ResponseEntity<String> getNewAccessTokenForLatestAbac(
            @RequestHeader(value = "Authorization") String oldAccessToken
    );

    @Operation(
        summary = "Refresh access token",
        description = "Generate a new access token using the current one",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "New access token generated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid authorization header",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or expired token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @Tag(name = "Token Management")
    ResponseEntity<ApiResponse<AccessTokenResponse>> getAccessTokenV1(
        @Parameter(description = "Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @RequestHeader(value = "Authorization") String oldAccessTokenHeader
    );

    @Operation(
        summary = "User logout",
        description = "Logout user from current device and revoke tokens",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Logout successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "Logout successful"
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid authorization header",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @Tag(name = "Authentication")
    ResponseEntity<ApiResponse<Void>> logout(
        @Parameter(description = "Bearer token", required = true, example = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @RequestHeader(value = "Authorization") String tokenHeader,
        @AuthenticationPrincipal Authentication authentication,
        HttpServletResponse response
    );

    @Operation(
        summary = "Verify email address",
        description = "Verify user's email address using verification token"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Email verified successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Invalid or expired token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @Tag(name = "Email Verification")
    ResponseEntity<ApiResponse<Void>> verifyEmail(
        @Parameter(description = "Email verification token", required = true)
        @RequestParam("token") String token
    );

    @Operation(
        summary = "Resend verification email",
        description = "Send verification email to user's registered email address"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Verification email sent",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "Verification email has been resent. Please check your email."
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @Tag(name = "Email Verification")
    ResponseEntity<ApiResponse<Void>> resendVerificationEmail(
        @Valid @RequestBody SendVerificationEmailRequest request,
        BindingResult bindingResult
    );

    @Operation(
        summary = "Change password",
        description = "Change user's password (requires authentication)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Password changed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "Password changed successfully"
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @Tag(name = "Password Management")
    ResponseEntity<ApiResponse<Void>> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        BindingResult bindingResult,
        @AuthenticationPrincipal UserDetails userDetails
    );

    @Operation(
        summary = "Request password reset",
        description = "Send password reset instructions to user's email"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Password reset email sent",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "Password reset instructions have been sent to your email"
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @Tag(name = "Password Management")
    ResponseEntity<ApiResponse<Void>> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request,
        BindingResult bindingResult
    );

    @Operation(
        summary = "Reset password",
        description = "Reset user's password using reset token"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Password reset successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                        "success": true,
                        "message": "Password has been reset successfully"
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation error or invalid token",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @Tag(name = "Password Management")
    ResponseEntity<ApiResponse<Void>> resetPassword(
        @Parameter(description = "Password reset token", required = true)
        @RequestParam("token") String token,
        @Valid @RequestBody ResetPasswordRequest request,
        BindingResult bindingResult
    );

    @Operation(
        summary = "OAuth2 callback",
        description = "Handle OAuth2 authentication callback",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "OAuth2 authentication successful",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        )
    })
    @Tag(name = "OAuth2")
    ResponseEntity<ApiResponse<LoginResponse>> getCurrentUserFromOAuth2(
        @AuthenticationPrincipal UserDetails userDetails,
        HttpServletRequest request
    );

    @Operation(
        summary = "Get user agent information",
        description = "Retrieve device and browser information from request"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "User agent information retrieved",
        content = @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = ApiResponse.class)
        )
    )
    @Tag(name = "Device Information")
    ResponseEntity<ApiResponse<UserAgentResponse>> getUserAgent(HttpServletRequest request);

    @Operation(
        summary = "CSRF token endpoint",
        description = "Endpoint for CSRF token generation/validation"
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "CSRF token handled successfully"
    )
    @Tag(name = "Security")
    ResponseEntity<Void> csrf();
}