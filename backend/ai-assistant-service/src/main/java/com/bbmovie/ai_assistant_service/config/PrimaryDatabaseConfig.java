package com.bbmovie.ai_assistant_service.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * This configuration class re-enables R2DBC repositories for the *primary*
 * (stable) database.
 * <p>
 * This is necessary because adding a manual @EnableR2dbcRepositories annotation
 * (like in _DatabaseConfig) disables Spring Boot's repository autoconfiguration.
 */
@SuppressWarnings("all")
@Configuration
@EnableR2dbcRepositories(
    basePackages = "com.bbmovie.ai_assistant_service.repository", // <-- 1. The package for your stable ChatSessionRepository
    entityOperationsRef = "r2dbcEntityTemplate" // <-- 2. The key! Point to the default autoconfigured bean
)
public class PrimaryDatabaseConfig {
    // This class just exists to hold the annotation.
    @Bean
    public ConnectionFactoryInitializer primaryDbInitializer(
            @Qualifier("r2dbcConnectionFactory") ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/schema.sql"));
        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}