package com.bbmovie.mediastreamingservice.controller;

import com.bbmovie.mediastreamingservice.service.StreamingService;
import jakarta.validation.constraints.Pattern;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stream")
public class StreamController {

    private final StreamingService streamingService;

    public static final String HLS_MIME_TYPE = "application/vnd.apple.mpegurl";

    // Master Playlist
    @GetMapping("/{movieId}/master.m3u8")
    public ResponseEntity<@NonNull Resource> getMasterPlaylist(
            @PathVariable UUID movieId,
            @AuthenticationPrincipal Jwt jwt) {
        String userTier = getUserTier(jwt);
        Resource filteredPlaylist = streamingService.getFilteredMasterPlaylist(movieId, userTier);
        return serveFile(filteredPlaylist, HLS_MIME_TYPE);
    }

    // Resolution Playlist
    @GetMapping("/{movieId}/{resolution}/playlist.m3u8")
    public ResponseEntity<@NonNull Resource> getResolutionPlaylist(
            @PathVariable UUID movieId,  // No need to regex, uuid safe
            @PathVariable @Pattern(regexp = "^(?:144|240|360|480|720|1080|1440|2160|4080)p$") String resolution,
            @AuthenticationPrincipal Jwt jwt) {
        String userTier = getUserTier(jwt);

        if (!streamingService.hasAccessToResolution(userTier, resolution)) {
            log.warn("User with tier {} attempted to access {} resolution playlist for movie {}", 
                    userTier, resolution, movieId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return serveFile(streamingService.getHlsFile(movieId, resolution), HLS_MIME_TYPE);
    }

    // Encryption Keys (Secure)
    @GetMapping("/keys/{movieId}/{resolution}/{keyFile}")
    public ResponseEntity<@NonNull Resource> getKey(
            @PathVariable UUID movieId, // No need to regex, uuid safe
            @PathVariable @Pattern(regexp = "^(?:144|240|360|480|720|1080|1440|2160|4080)p$") String resolution,
            @PathVariable @Pattern(regexp = "^key_\\d+\\.key$") String keyFile,
            @AuthenticationPrincipal Jwt jwt) {
        String userTier = getUserTier(jwt);

        if (!streamingService.hasAccessToResolution(userTier, resolution)) {
            log.warn("User with tier {} attempted to access {} resolution key for movie {}", 
                    userTier, resolution, movieId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return serveFile(streamingService.getSecureKey(movieId, resolution, keyFile), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private ResponseEntity<@NonNull Resource> serveFile(Resource resource, String contentType) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache") // HLS files often shouldn't be cached too long
                .body(resource);
    }

    /**
     * Extracts the user subscription tier from JWT token.
     * Defaults to "FREE" if tier is not present or blank.
     *
     * @param jwt The JWT token
     * @return The user's subscription tier
     */
    private String getUserTier(Jwt jwt) {
        String userTier = jwt.getClaimAsString(SUBSCRIPTION_TIER);
        if (userTier == null || userTier.isBlank()) {
            return "FREE"; // Default to FREE if tier is not present
        }
        return userTier;
    }
}
