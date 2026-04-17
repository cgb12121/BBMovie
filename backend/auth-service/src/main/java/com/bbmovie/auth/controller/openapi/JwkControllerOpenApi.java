package com.bbmovie.auth.controller.openapi;

import com.bbmovie.auth.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@SuppressWarnings("unused")
@Tag(name = "JWK", description = "JSON Web Key endpoints for token verification and key management")
public interface JwkControllerOpenApi {
    @Operation(summary = "Get public JWK set", description = "Public endpoint exposing active JWK set")
    ResponseEntity<Map<String, Object>> getJwk();

    @Operation(summary = "Get all admin JWKs", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<Map<String, Object>> getJwks();

    @Operation(summary = "Get active admin JWK", security = @SecurityRequirement(name = "bearerAuth"))
    ApiResponse<Map<String, Object>> getActiveJwks();
}

