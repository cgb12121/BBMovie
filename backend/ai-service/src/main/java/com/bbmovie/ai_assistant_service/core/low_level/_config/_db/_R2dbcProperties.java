package com.bbmovie.ai_assistant_service.core.low_level._config._db;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A simple POJO (Data Class) to hold the configuration properties for the
 *  datasource. Spring Boot's @ConfigurationProperties will
 * bind values to this object using its setter methods.
 */
@Data
@Component
@ConfigurationProperties("ai.datasource")
public class _R2dbcProperties {
    private String driver;
    private String host;
    private Integer port = 3306;
    private String username;
    private String password;
    private String database;
    private boolean loadSamples;
}