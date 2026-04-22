package com.bbmovie.revenuedashboard.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class NatsConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "revenue.analytics.messaging.nats.enabled", havingValue = "true")
    public Connection natsConnection(RevenueAnalyticsProperties properties) throws IOException, InterruptedException {
        var options = new io.nats.client.Options.Builder()
                .server(properties.getMessaging().getNats().getUrl())
                .connectionTimeout(Duration.ofSeconds(5))
                .reconnectWait(Duration.ofSeconds(2))
                .maxReconnects(-1)
                .build();

        return Nats.connect(options);
    }
}
