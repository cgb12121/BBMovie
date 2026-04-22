package com.bbmovie.homepagerecommendations.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "homepage.recommendations")
public class HomepageRecommendationsProperties {

    private final Trending trending = new Trending();
    private final Nats nats = new Nats();

    @Getter
    @Setter
    public static class Trending {
        private String dailyKeyPrefix = "homepage:trending:day";
        private int mergeWindowDays = 7;
        private int dailyKeyTtlDays = 8;
        private double gravity = 1.8;
        private double completedDelta = 3.0;
        private double partialDelta = 1.0;
        private String zoneId = "UTC";
        private String unionTempKeyPrefix = "homepage:trending:union:tmp:";
        private final Batch batch = new Batch();

        @Getter
        @Setter
        public static class Batch {
            private boolean enabled = true;
            private long flushIntervalMs = 5000L;
            private int maxBufferSize = 500;
        }
    }

    @Getter
    @Setter
    public static class Nats {
        private boolean enabled;
        private String url = "nats://localhost:4222";
        private String streamName = "BBMOVIE_PLAYBACK";
        private String subject = "playback.tracking.v1";
        private String consumerDurable = "homepage-recommendations-playback";
    }
}
