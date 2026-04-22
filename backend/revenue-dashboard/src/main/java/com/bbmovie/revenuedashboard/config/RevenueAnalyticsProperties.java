package com.bbmovie.revenuedashboard.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "revenue.analytics")
@Getter
@Setter
public class RevenueAnalyticsProperties {

    private final Clickhouse clickhouse = new Clickhouse();
    private final Messaging messaging = new Messaging();

    @Getter
    @Setter
    public static class Clickhouse {
        private boolean enabled;
        private String jdbcUrl = "jdbc:clickhouse://localhost:8123/bbmovie_analytics";
        private String username = "default";
        private String password = "";
    }

    @Getter
    @Setter
    public static class Messaging {
        private final Nats nats = new Nats();
    }

    @Getter
    @Setter
    public static class Nats {
        private String url = "nats://localhost:4222";
        private String streamName = "revenue_events";
        private String consumerName = "revenue-dashboard-consumer";
        private String deliverPolicy = "all";
    }
}
