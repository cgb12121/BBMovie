package com.bbmovie.auth.controller.openapi;

import com.bbmovie.auth.dto.ApiResponse;
import com.bbmovie.auth.dto.request.RevokeDeviceRequest;
import com.bbmovie.auth.dto.response.LoggedInDeviceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@Tag(name = "Session Management", description = "Device session management APIs")
public interface SessionControllerOpenApi {

    @Operation(summary = "Session API test", description = "Simple endpoint to verify session controller availability")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successful response")
    ResponseEntity<ApiResponse<String>> test();

    @Operation(
            summary = "List logged-in devices",
            description = "Returns all active devices/sessions for the current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Devices retrieved successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    ResponseEntity<ApiResponse<List<LoggedInDeviceResponse>>> getAllDeviceLoggedIntoAccount(
            @Parameter(description = "Bearer token", required = true)
            @RequestHeader("Authorization") String accessToken,
            HttpServletRequest request
    );

    @Operation(
            summary = "Revoke device sessions",
            description = "Revokes one or more logged-in device sessions for the current user",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Device sessions revoked",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    ResponseEntity<ApiResponse<Map<String, String>>> revokeDeviceLoggedIntoAccount(
            @Parameter(description = "Bearer token", required = true)
            @RequestHeader("Authorization") String accessToken,
            @RequestBody RevokeDeviceRequest request
    );
}
