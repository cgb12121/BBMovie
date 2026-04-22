package com.bbmovie.mediastreamingservice.controller;

import com.bbmovie.mediastreamingservice.controller.openapi.DirectMediaStreamOpenApi;
import com.bbmovie.mediastreamingservice.service.DirectMediaStreamService;
import com.bbmovie.mediastreamingservice.utils.JwtUtils;
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

import java.net.URI;
import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stream/direct")
public class DirectMediaStreamController implements DirectMediaStreamOpenApi {

    private final DirectMediaStreamService directMediaStreamService;

    public static final String HLS_MIME_TYPE = "application/vnd.apple.mpegurl";

    @GetMapping("/{movieId}/master.m3u8")
    public ResponseEntity<@NonNull Resource> getMasterPlaylist(
            @PathVariable UUID movieId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.getUserId(jwt);
        Resource resource = directMediaStreamService.getFilteredMasterPlaylistWithPresignedVariants(movieId, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(HLS_MIME_TYPE))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }

    @GetMapping("/{movieId}/{resolution}/playlist.m3u8")
    public ResponseEntity<Void> getResolutionPlaylistRedirect(
            @PathVariable UUID movieId,
            @PathVariable String resolution,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.getUserId(jwt);
        String url = directMediaStreamService.presignResolutionPlaylistUrl(movieId, resolution, userId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }

    @GetMapping("/keys/{movieId}/{resolution}/{keyFile}")
    public ResponseEntity<Void> getKeyRedirect(
            @PathVariable UUID movieId,
            @PathVariable String resolution,
            @PathVariable String keyFile,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.getUserId(jwt);
        String url = directMediaStreamService.presignSecureKeyUrl(movieId, resolution, keyFile, userId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }
}
