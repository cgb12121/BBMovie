package com.bbmovie.mediastreamingservice.controller;

import com.bbmovie.mediastreamingservice.service.StreamingService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stream")
public class StreamController {

    private final StreamingService streamingService;

    // Master Playlist
    @GetMapping("/{movieId}/master.m3u8")
    public ResponseEntity<Resource> getMasterPlaylist(@PathVariable String movieId) {
        String objectKey = "movies/" + movieId + "/master.m3u8";
        return serveFile(streamingService.getHlsFile(objectKey), "application/vnd.apple.mpegurl");
    }

    // Resolution Playlist
    @GetMapping("/{movieId}/{resolution}/playlist.m3u8")
    public ResponseEntity<Resource> getResolutionPlaylist(@PathVariable String movieId, @PathVariable String resolution) {
        String objectKey = "movies/" + movieId + "/" + resolution + "/playlist.m3u8";
        return serveFile(streamingService.getHlsFile(objectKey), "application/vnd.apple.mpegurl");
    }

    // Video Segments
    @GetMapping("/segments/{movieId}/{resolution}/{segment}")
    public ResponseEntity<Resource> getSegment(@PathVariable String movieId, @PathVariable String resolution, @PathVariable String segment) {
        String objectKey = "movies/" + movieId + "/" + resolution + "/" + segment;
        return serveFile(streamingService.getHlsFile(objectKey), "video/MP2T");
    }

    // Encryption Keys (Secure) - TODO: Add Security/Auth check here
    @GetMapping("/keys/{movieId}/{resolution}/{keyFile}")
    public ResponseEntity<Resource> getKey(
            @PathVariable String movieId, @PathVariable String resolution, @PathVariable String keyFile,
            @AuthenticationPrincipal Jwt jwt) {
        String objectKey = "movies/" + movieId + "/" + resolution + "/" + keyFile;
        // In real impl, check user subscription here
        return serveFile(streamingService.getSecureKey(objectKey), "application/octet-stream");
    }

    private ResponseEntity<Resource> serveFile(Resource resource, String contentType) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache") // HLS files often shouldn't be cached too long
                .body(resource);
    }
}
