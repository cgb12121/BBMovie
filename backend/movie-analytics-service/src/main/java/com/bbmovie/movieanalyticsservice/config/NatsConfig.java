package com.bbmovie.movieanalyticsservice.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class NatsConfig {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(name = "movie.analytics.messaging.nats.enabled", havingValue = "true")
    public Connection natsConnection() throws IOException, InterruptedException {
        return Nats.connect();
    }
}

