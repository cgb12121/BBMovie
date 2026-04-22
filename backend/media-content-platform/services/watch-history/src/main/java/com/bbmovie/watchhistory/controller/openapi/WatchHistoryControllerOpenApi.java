package com.bbmovie.watchhistory.controller.openapi;

import com.bbmovie.watchhistory.dto.PlaybackTrackRequest;
import com.bbmovie.watchhistory.dto.ResumeListPageResponse;
import com.bbmovie.watchhistory.dto.ResumeResponse;
import com.bbmovie.watchhistory.dto.TrackPlaybackResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Watch History", description = "Playback tracking and resume state APIs")
public interface WatchHistoryControllerOpenApi {
    @Operation(summary = "Track playback", security = @SecurityRequirement(name = "bearerAuth"))
    TrackPlaybackResponse trackPlayback(@Valid @RequestBody PlaybackTrackRequest request, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "Get resume point", security = @SecurityRequirement(name = "bearerAuth"))
    ResponseEntity<ResumeResponse> resume(@PathVariable UUID movieId, @AuthenticationPrincipal Jwt jwt);

    @Operation(summary = "List resume items", security = @SecurityRequirement(name = "bearerAuth"))
    ResumeListPageResponse listResumeItems(
            @RequestParam(name = "cursor", defaultValue = "0") String cursor,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            @AuthenticationPrincipal Jwt jwt
    );
}

