package com.bbmovie.mediastreamingservice.controller;

import com.bbmovie.mediastreamingservice.controller.openapi.ProxyMediaStreamOpenApi;
import com.bbmovie.mediastreamingservice.service.ProxyMediaStreamService;
import com.bbmovie.mediastreamingservice.utils.JwtUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
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

@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stream")
public class ProxyMediaStreamController implements ProxyMediaStreamOpenApi {

    private final ProxyMediaStreamService proxyMediaStreamService;

    public static final String HLS_MIME_TYPE = "application/vnd.apple.mpegurl";

    @GetMapping("/{movieId}/master.m3u8")
    public ResponseEntity<@NonNull Resource> getMasterPlaylist(
            @PathVariable UUID movieId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.getUserId(jwt);
        Resource filteredPlaylist = proxyMediaStreamService.getFilteredMasterPlaylist(movieId, userId);
        return serveFile(filteredPlaylist, HLS_MIME_TYPE);
    }

    @GetMapping("/{movieId}/{resolution}/playlist.m3u8")
    public ResponseEntity<@NonNull Resource> getResolutionPlaylist(
            @PathVariable UUID movieId,
            @PathVariable String resolution,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.getUserId(jwt);
        return serveFile(proxyMediaStreamService.getHlsFile(movieId, resolution, userId), HLS_MIME_TYPE);
    }

    @GetMapping("/keys/{movieId}/{resolution}/{keyFile}")
    public ResponseEntity<@NonNull Resource> getKey(
            @PathVariable UUID movieId,
            @PathVariable String resolution,
            @PathVariable String keyFile,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.getUserId(jwt);
        return serveFile(proxyMediaStreamService.getSecureKey(movieId, resolution, keyFile, userId), MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    private ResponseEntity<@NonNull Resource> serveFile(Resource resource, String contentType) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }
}
