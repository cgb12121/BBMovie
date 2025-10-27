package com.bbmovie.ai_assistant_service._experimental._low_level._config;

import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * A simple POJO (Data Class) to hold the configuration properties for the
 * experimental datasource. Spring Boot's @ConfigurationProperties will
 * bind values to this object using its setter methods.
 * <p>
 * We do NOT put @ConfigurationProperties here, as we are creating it
 * as a @Bean in the _DatabaseConfig class.
 */
@Data
@Component
@ConfigurationProperties("ai.experimental.datasource")
@ConditionalOnBooleanProperty(name = "ai.experimental.enabled")
public class _R2dbcProperties {
    private String driver = "mysql"; // Default driver
    private String host = "localhost";
    private Integer port = 3306;     // Default port
    private String username;
    private String password;
    private String database = "bbmovie_experimental"; // Default database
}