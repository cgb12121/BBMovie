package com.bbmovie.ai_assistant_service.core.low_level._config._db;

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
 * Configuration for the secondary R2DBC database.
 * <p>
 * We must explicitly define all beans (@Qualifier) and tell the annotations
 * (@EnableR2dbcRepositories, @EnableR2dbcAuditing) which beans are to use
 * to avoid conflicts with the primary database autoconfiguration.
 */
@Configuration
@EnableR2dbcRepositories(
        basePackages = "com.bbmovie.ai_assistant_service.core.low_level._database",
        entityOperationsRef = "_EntityOperations"
)
@EnableR2dbcAuditing
public class _DatabaseConfig {
    /**
     * Creates the ConnectionFactory for the experimental DB.
     * We use @Qualifier to make it unique.
     */
    @Bean
    @Qualifier("_ConnectionFactory")
    public ConnectionFactory _ConnectionFactory(_R2dbcProperties properties) {
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
    @Qualifier("_DatabaseClient")
    public DatabaseClient _DatabaseClient(
            @Qualifier("_ConnectionFactory") ConnectionFactory connectionFactory) {
        return DatabaseClient.create(connectionFactory);
    }

    @Bean
    @Qualifier("_EntityOperations")
    public R2dbcEntityOperations _EntityOperations(
            @Qualifier("_DatabaseClient") DatabaseClient databaseClient,
            @Qualifier("_ConnectionFactory") ConnectionFactory connectionFactory) {

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
    @Qualifier("_TransactionManager")
    public ReactiveTransactionManager _TransactionManager(
            @Qualifier("_ConnectionFactory") ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    /**
     * Initializes the experimental database schema on startup.
     * It runs the '_schema.sql' script.
     */
    @Bean
    @Qualifier("_DbInitializer")
    public ConnectionFactoryInitializer _DbInitializer(
            @Qualifier("_ConnectionFactory") ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("db/_schema.sql"));
        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}
