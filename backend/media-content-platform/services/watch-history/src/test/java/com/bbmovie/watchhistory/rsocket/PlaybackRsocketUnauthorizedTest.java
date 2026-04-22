package com.bbmovie.watchhistory.rsocket;

import com.bbmovie.watchhistory.dto.PlaybackTrackRequest;
import com.bbmovie.watchhistory.dto.TrackPlaybackResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(
        properties = {
            "eureka.client.enabled=false",
            "spring.rsocket.server.port=18097"
        })
class PlaybackRsocketUnauthorizedTest {

    private static final int RSOCKET_TEST_PORT = 18097;

    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RSocketRequester.Builder requesterBuilder;

    @Test
    void trackWithoutJwtMetadataIsRejected() {
        PlaybackTrackRequest req = new PlaybackTrackRequest();
        req.setMovieId(UUID.randomUUID());
        req.setPositionSec(1.0);

        RSocketRequester requester = requesterBuilder.tcp("localhost", RSOCKET_TEST_PORT);

        assertThrows(
                Exception.class,
                () -> requester
                        .route("playback.track")
                        .data(req)
                        .retrieveMono(TrackPlaybackResponse.class)
                        .block());
    }
}
