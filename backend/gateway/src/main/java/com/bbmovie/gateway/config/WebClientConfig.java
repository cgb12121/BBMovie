package com.bbmovie.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${internal.url.auth.base-url}")
    private String authServiceUrl;

    @LoadBalanced
    @Bean("authWebClient")
    public WebClient authWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(authServiceUrl)
                .defaultHeader("Accept", "application/json")
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(5))))
                .build();
    }
}
