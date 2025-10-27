package com.bbmovie.ai_assistant_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

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
}