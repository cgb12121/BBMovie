package com.bbmovie.search.config.vector;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.search.engine", havingValue = "qdrant", matchIfMissing = true)
public class QdrantConfig {

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6334}") // Port gRPC
    private int port;
    
    @Value("${qdrant.api-key:}")
    private String apiKey;

    /**
     * Init QdrantClient
     * gRPC client is lazy connection, so new QdrantClient() normally won't cause a crash
     */
    @Bean(destroyMethod = "close")
    public QdrantClient qdrantClient() {
        try {
            QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, !apiKey.isBlank());
            if (!apiKey.isBlank()) {
                builder.withApiKey(apiKey);
            }
            
            QdrantClient client = new QdrantClient(builder.build());
            log.info("QdrantClient initialized successfully (Lazy connection)");
            return client;
        } catch (Exception e) {
            log.error("Failed to initialize QdrantClient. Search features will be disabled!", e);
            throw  new RuntimeException("Failed to initialize QdrantClient", e);
        }
    }
}