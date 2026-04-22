package com.bbmovie.watchhistory.controller;

import com.bbmovie.watchhistory.controller.openapi.WatchHistoryControllerOpenApi;
import com.bbmovie.watchhistory.dto.PlaybackTrackRequest;
import com.bbmovie.watchhistory.dto.ResumeListPageResponse;
import com.bbmovie.watchhistory.dto.ResumeResponse;
import com.bbmovie.watchhistory.dto.TrackPlaybackResponse;
import com.bbmovie.watchhistory.service.WatchHistoryTrackingService;
import com.bbmovie.watchhistory.util.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/watch-history/v1")
public class WatchHistoryController implements WatchHistoryControllerOpenApi {

    private final WatchHistoryTrackingService trackingService;

    @PostMapping("/playback")
    public TrackPlaybackResponse trackPlayback(
            @Valid @RequestBody PlaybackTrackRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.getSubject(jwt);
        return trackingService.track(userId, request);
    }

    @GetMapping("/resume/{movieId}")
    public ResponseEntity<ResumeResponse> resume(
            @PathVariable UUID movieId,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.getSubject(jwt);
        return trackingService
                .getResume(userId, movieId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/items")
    public ResumeListPageResponse listResumeItems(
            @RequestParam(name = "cursor", defaultValue = "0") String cursor,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = JwtUtils.getSubject(jwt);
        return trackingService.listResumeStatesPage(userId, cursor, limit);
    }
}
