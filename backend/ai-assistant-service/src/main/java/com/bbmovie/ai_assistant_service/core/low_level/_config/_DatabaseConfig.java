package com.bbmovie.ai_assistant_service.core.low_level._config;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * Configuration for the "experimental" secondary R2DBC database.
 * <p>
 * We must explicitly define all beans (@Qualifier) and tell the annotations
 * (@EnableR2dbcRepositories, @EnableR2dbcAuditing) which beans are to use
 * to avoid conflicts with the primary database autoconfiguration.
 */
@Configuration
@EnableR2dbcRepositories(
        basePackages = "com.bbmovie.ai_assistant_service.core.low_level._database",
        entityOperationsRef = "experimentalEntityOperations" // <-- Key Fix 1: Point to the correct DatabaseClient
)
@EnableR2dbcAuditing
public class _DatabaseConfig {
    /**
     * Creates the ConnectionFactory for the experimental DB.
     * We use @Qualifier to make it unique.
     */
    @Bean
    @Qualifier("experimentalConnectionFactory")
    public ConnectionFactory experimentalConnectionFactory(_R2dbcProperties properties) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, properties.getDriver())
                .option(ConnectionFactoryOptions.HOST, properties.getHost())
                .option(ConnectionFactoryOptions.PORT, properties.getPort())
                .option(ConnectionFactoryOptions.USER, properties.getUsername())
                .option(ConnectionFactoryOptions.PASSWORD, properties.getPassword())
                .option(ConnectionFactoryOptions.DATABASE, properties.getDatabase())
                .build();
        // Use ConnectionFactories (plural) to get the provider
        return ConnectionFactories.get(options);
    }

    /**
     * Creates a DatabaseClient for the experimental DB.
     * This is what repositories use to execute queries.
     * We MUST @Qualifier the ConnectionFactory it depends on.
     */
    @Bean
    @Qualifier("experimentalDatabaseClient")
    public DatabaseClient experimentalDatabaseClient(
            @Qualifier("experimentalConnectionFactory") ConnectionFactory connectionFactory) {
        return DatabaseClient.create(connectionFactory);
    }

    @Bean
    @Qualifier("experimentalEntityOperations")
    public R2dbcEntityOperations experimentalEntityOperations(
            @Qualifier("experimentalDatabaseClient") DatabaseClient databaseClient,
            @Qualifier("experimentalConnectionFactory") ConnectionFactory connectionFactory) {

        // Resolve the dialect from the connection factory
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);

        // R2dbcEntityTemplate is the standard implementation of R2dbcEntityOperations
        return new R2dbcEntityTemplate(databaseClient, dialect);
    }

    /**
     * Creates a TransactionManager for the experimental DB.
     * We MUST @Qualifier the ConnectionFactory it depends on.
     */
    @Bean
    @Qualifier("experimentalTransactionManager")
    public ReactiveTransactionManager experimentalTransactionManager(
            @Qualifier("experimentalConnectionFactory") ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    /**
     * Initializes the experimental database schema on startup.
     * It runs the 'experimental_schema.sql' script.
     */
    @Bean
    @Qualifier("experimentalDbInitializer")
    public ConnectionFactoryInitializer experimentalDbInitializer(
            @Qualifier("experimentalConnectionFactory") ConnectionFactory connectionFactory
    ) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/experimental_schema.sql"));
        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}
