package com.bbmovie.transcodeworker.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for MinIO object storage client.
 * Sets up the MinIO client with connection parameters from configuration.
 */
@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String url;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    /**
     * Creates and returns a MinIO client bean instance.
     *
     * @return MinioClient instance configured with the provided connection parameters
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
            .endpoint(url)
            .credentials(accessKey, secretKey)
            .build();
    }
}
