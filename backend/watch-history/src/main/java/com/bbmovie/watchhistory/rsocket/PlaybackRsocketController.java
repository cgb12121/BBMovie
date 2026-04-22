package com.bbmovie.watchhistory.rsocket;

import com.bbmovie.watchhistory.dto.PlaybackTrackRequest;
import com.bbmovie.watchhistory.dto.TrackPlaybackResponse;
import com.bbmovie.watchhistory.service.WatchHistoryTrackingService;
import com.bbmovie.watchhistory.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * RSocket request–response route (same business logic as HTTP {@code POST /playback}).
 * <p>Clients must attach a JWT using Bearer token metadata on SETUP, for example:
 * {@code BearerTokenMetadata} + {@code WellKnownMimeType.MESSAGE_RSOCKET_AUTHENTICATION} with
 * {@link org.springframework.messaging.rsocket.RSocketRequester.Builder#setupMetadata(Object, org.springframework.util.MimeType)}.
 * </p>
 */
@Controller
@RequiredArgsConstructor
public class PlaybackRsocketController {

    private final WatchHistoryTrackingService trackingService;

    @MessageMapping("playback.track")
    public Mono<TrackPlaybackResponse> track(
            @Payload PlaybackTrackRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (request.getMovieId() == null || request.getPositionSec() == null) {
            return Mono.error(new IllegalArgumentException("movieId and positionSec are required"));
        }
        String userId = JwtUtils.getSubject(jwt);
        return Mono.fromCallable(() -> trackingService.track(userId, request))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
