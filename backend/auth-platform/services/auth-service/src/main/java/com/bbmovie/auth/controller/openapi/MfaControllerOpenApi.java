package com.bbmovie.auth.controller.openapi;

import com.bbmovie.auth.dto.request.MfaVerifyRequest;
import com.bbmovie.auth.dto.response.MfaSetupResponse;
import com.bbmovie.auth.dto.response.MfaVerifyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "MFA", description = "Multi-factor authentication setup and verification APIs")
public interface MfaControllerOpenApi {

    @Operation(
            summary = "Setup MFA",
            description = "Generates MFA secret and QR code for the authenticated user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MFA setup details generated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MfaSetupResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    ResponseEntity<MfaSetupResponse> setupMfa(
            @Parameter(description = "Bearer token", required = true)
            @RequestHeader("Authorization") String authorizationHeader
    );

    @Operation(
            summary = "Verify MFA code",
            description = "Validates TOTP code and enables MFA for current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MFA enabled successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = MfaVerifyResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid MFA code", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    ResponseEntity<MfaVerifyResponse> verifyMfa(
            @Parameter(description = "Bearer token", required = true)
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody MfaVerifyRequest request
    );
}
