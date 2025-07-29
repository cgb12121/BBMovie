package com.bbmovie.gateway.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    private static final String AUTH_SERVICE_URL =  "http://localhost:8080";

    @Bean
    @LoadBalanced
    public WebClient authWebClient(WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .baseUrl(AUTH_SERVICE_URL)
                .defaultHeader("Accept", "application/json")
                .clientConnector(
                        new ReactorClientHttpConnector(
                                HttpClient.create()
                                        .responseTimeout(Duration.ofSeconds(5))
                        )
                )
                .build();
    }
}
