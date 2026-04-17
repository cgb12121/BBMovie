package com.bbmovie.mediastreamingservice.controller.openapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.NonNull;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@SuppressWarnings("unused")
@Tag(name = "Streaming", description = "HLS streaming endpoints")
public interface StreamControllerOpenApi {
    String RESOLUTION_PATTERN = "^(?:144|240|360|480|720|1080|1440|2160|4080)p$";
    String KEY_FILE_PATTERN = "^key_\\d+\\.key$";

    @Operation(summary = "Get master playlist", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull Resource> getMasterPlaylist(@PathVariable UUID movieId, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Get resolution playlist", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull Resource> getResolutionPlaylist(
            @PathVariable UUID movieId,
            @PathVariable @Pattern(regexp = RESOLUTION_PATTERN) String resolution,
            @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Get encryption key", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<@NonNull Resource> getKey(
            @PathVariable UUID movieId,
            @PathVariable @Pattern(regexp = RESOLUTION_PATTERN) String resolution,
            @PathVariable @Pattern(regexp = KEY_FILE_PATTERN) String keyFile,
            @AuthenticationPrincipal Jwt jwt);
}

