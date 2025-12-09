package com.bbmovie.ai_assistant_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;

@Configuration
public class RefineryClientConfig {

    @Value("${rust.ai.service.url:lb://AI-REFINERY}")
    private String refineryUrl;

    @Bean("rustWebClient")
    public WebClient rustWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(5)) // Whisper can take time
                .option(CONNECT_TIMEOUT_MILLIS, 30000);

        return builder
                .baseUrl(refineryUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
