package com.bbmovie.homepagerecommendations.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(name = "homepage.recommendations.nats.enabled", havingValue = "true")
public class HomepageNatsConfiguration {

    @Bean(destroyMethod = "close")
    public Connection homepageNatsConnection(HomepageRecommendationsProperties properties) throws IOException, InterruptedException {
        return Nats.connect(properties.getNats().getUrl());
    }
}
