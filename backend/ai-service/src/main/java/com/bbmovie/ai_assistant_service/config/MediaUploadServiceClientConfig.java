package com.bbmovie.ai_assistant_service.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuration for WebClient to communicate with the media-upload-service.
 * <p>
 * This configures a reactive WebClient that can be used to get file download URLs
 * from the media-upload-service. The client is configured with:
 * - Connection timeout: 5 seconds (for quick URL lookups)
 * - Response timeout: 10 seconds (for file metadata retrieval)
 * - Uses service discovery through Eureka (lb://media-upload-service)
 */
@Configuration
public class MediaUploadServiceClientConfig {

    @Value("${media-upload-service.url:lb://media-upload-service}")
    private String mediaUploadServiceUrl;

    @Bean("mediaUploadServiceWebClient")
    public WebClient mediaUploadServiceWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);

        return WebClient.builder()
                .baseUrl(mediaUploadServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

