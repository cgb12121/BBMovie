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
 * Configuration for WebClient to communicate with the file-service.
 * <p>
 * This configures a reactive WebClient that can be used to upload files
 * to the file-service microservice. The client is configured with:
 * - Connection timeout: 30 seconds (for large file uploads)
 * - Response timeout: 60 seconds (for file processing)
 * - Uses service discovery through Eureka (lb://file-service)
 * <p>
 * Why this approach:
 * - WebClient is the reactive, non-blocking HTTP client for Spring WebFlux
 * - Service discovery (lb://) allows the gateway to route requests automatically
 * - Timeouts are set appropriately for file upload operations
 */
@Configuration
public class FileServiceClientConfig {

    @Value("${file-service.url:lb://file-service}")
    private String fileServiceUrl;

    @Bean
    public WebClient fileServiceWebClient() {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000);

        return WebClient.builder()
                .baseUrl(fileServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}

