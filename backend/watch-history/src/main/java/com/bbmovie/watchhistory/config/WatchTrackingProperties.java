package com.bbmovie.watchhistory.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "watch-history.tracking")
public class WatchTrackingProperties {

    private final Redis redis = new Redis();
    private final Nats nats = new Nats();

    private int segmentDurationSec = 10;

    /**
     * Hint for clients that batch HTTP/WebSocket updates (Level 2). Server still accepts any cadence.
     */
    private int suggestedClientFlushSec = 30;

    @Getter
    @Setter
    public static class Redis {
        private String posKeyPattern = "user:pos:%s";
        private String segmentKeyPattern = "user:lastseg:%s:%s";
        private int posTtlDays = 30;
        private int segmentTtlHours = 2;
    }

    @Getter
    @Setter
    public static class Nats {
        private boolean enabled;
        private String url = "nats://localhost:4222";
        private String subject = "playback.tracking.v1";
    }
}
