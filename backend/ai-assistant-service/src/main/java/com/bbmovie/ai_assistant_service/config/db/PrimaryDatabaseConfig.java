package com.bbmovie.ai_assistant_service.config.db;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;

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

    @Bean
    @Primary
    @Qualifier("r2dbcConnectionFactory")
    public ConnectionFactory primaryConnectionFactory(R2dbcProperties properties) {
        // Use the properties bound from "spring.r2dbc.*"
        ConnectionFactoryOptions options = ConnectionFactoryOptions.parse(properties.getUrl())
                .mutate()
                .option(ConnectionFactoryOptions.USER, properties.getUsername())
                .option(ConnectionFactoryOptions.PASSWORD, properties.getPassword())
                .build();

        return ConnectionFactories.get(options);
    }

    @Bean
    @Primary
    @Qualifier("r2dbcEntityTemplate")
    public R2dbcEntityOperations primaryEntityOperations(
            @Qualifier("databaseClient") DatabaseClient databaseClient,
            @Qualifier("r2dbcConnectionFactory") ConnectionFactory connectionFactory) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        return new R2dbcEntityTemplate(databaseClient, dialect);
    }

    @Bean
    @Primary
    @Qualifier("transactionManager") // "transactionManager" is the default name
    public ReactiveTransactionManager primaryTransactionManager(
            @Qualifier("r2dbcConnectionFactory") ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    @Primary
    @Qualifier("databaseClient") // "databaseClient" is a common default name
    public DatabaseClient primaryDatabaseClient(
            @Qualifier("r2dbcConnectionFactory") ConnectionFactory connectionFactory) {
        return DatabaseClient.create(connectionFactory);
    }

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