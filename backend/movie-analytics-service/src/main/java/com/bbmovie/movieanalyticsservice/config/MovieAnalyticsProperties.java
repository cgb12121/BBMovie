package com.bbmovie.movieanalyticsservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "movie.analytics")
public class MovieAnalyticsProperties {

    private final Messaging messaging = new Messaging();
    private final Heatmap heatmap = new Heatmap();
    private final Clickhouse clickhouse = new Clickhouse();

    @Getter
    @Setter
    public static class Messaging {
        private final Kafka kafka = new Kafka();
        private final Nats nats = new Nats();
    }

    @Getter
    @Setter
    public static class Kafka {
        private boolean enabled;
        private String topic = "analytics.heatmap.raw";
    }

    @Getter
    @Setter
    public static class Nats {
        private boolean enabled;
        private String subject = "analytics.heatmap.raw";
    }

    @Getter
    @Setter
    public static class Heatmap {
        private int bucketSizeDefault = 10;
        private int maxBuckets = 720;
        private String redisPrefix = "movie:heatmap";
    }

    @Getter
    @Setter
    public static class Clickhouse {
        private boolean enabled;
        private String jdbcUrl = "jdbc:clickhouse://localhost:8123/default";
        private String username = "default";
        private String password = "";
    }
}

