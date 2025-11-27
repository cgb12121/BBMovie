package com.bbmovie.ai_assistant_service.config.db;

import com.bbmovie.ai_assistant_service.config.db.converter.StringToUuidConverter;
import com.bbmovie.ai_assistant_service.config.db.converter.UuidToStringConverter;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableR2dbcRepositories(
        basePackages = "com.bbmovie.ai_assistant_service.repository",
        entityOperationsRef = "entityOperations"
)
@EnableTransactionManagement
@EnableR2dbcAuditing
public class R2dbcDatabaseConfig {

    private final RgbLogger log = RgbLoggerFactory.getLogger(R2dbcDatabaseConfig.class);

    @Bean
    @Qualifier("connectionFactory")
    public ConnectionFactory connectionFactory(
            R2dbcProperties properties, ObjectProvider<ProxyExecutionListener> listenerProvider) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
                .option(ConnectionFactoryOptions.DRIVER, properties.getDriver())
                .option(ConnectionFactoryOptions.HOST, properties.getHost())
                .option(ConnectionFactoryOptions.PORT, properties.getPort())
                .option(ConnectionFactoryOptions.USER, properties.getUsername())
                .option(ConnectionFactoryOptions.PASSWORD, properties.getPassword())
                .option(ConnectionFactoryOptions.DATABASE, properties.getDatabase())
                .build();

        ConnectionFactory base = ConnectionFactories.get(options);

        ProxyExecutionListener listener = listenerProvider.getIfAvailable();

        if (listener == null) {
            return ProxyConnectionFactory
                    .builder(base)
                    .build();
        }

        return ProxyConnectionFactory.builder(base)
                .listener(listener)
                .build();
    }

    @Bean
    @Qualifier("customConversions")
    public R2dbcCustomConversions customConversions() {
        List<Object> converters = new ArrayList<>();
        converters.add(UuidToStringConverter.INSTANCE);
        converters.add(StringToUuidConverter.INSTANCE);
        return new R2dbcCustomConversions(R2dbcCustomConversions.StoreConversions.NONE, converters);
    }

    @Bean
    @Qualifier("e2dbcConverter")
    public R2dbcConverter e2dbcConverter(
            @Qualifier("customConversions") R2dbcCustomConversions customConversions) {
        R2dbcMappingContext mappingContext = new R2dbcMappingContext();
        mappingContext.afterPropertiesSet();
        return new MappingR2dbcConverter(mappingContext, customConversions);
    }

    @Bean
    @Qualifier("databaseClient")
    public DatabaseClient databaseClient(@Qualifier("connectionFactory") ConnectionFactory connectionFactory) {
        return DatabaseClient.create(connectionFactory);
    }

    @Bean
    @Qualifier("entityOperations")
    public R2dbcEntityOperations entityOperations(
            @Qualifier("databaseClient") DatabaseClient databaseClient,
            @Qualifier("connectionFactory") ConnectionFactory connectionFactory,
            @Qualifier("r2dbcConverter") R2dbcConverter r2dbcConverter) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        return new R2dbcEntityTemplate(databaseClient, dialect, r2dbcConverter);
    }

    @Bean
    @Qualifier("transactionManager")
    public ReactiveTransactionManager transactionManager(
            @Qualifier("connectionFactory") ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    @Qualifier("dbInitializer")
    public ConnectionFactoryInitializer dbInitializerWithSchemaValidation(
            R2dbcProperties properties, @Qualifier("connectionFactory") ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator schemaPopulator = new ResourceDatabasePopulator();
        schemaPopulator.addScript(new ClassPathResource("db/_schema.sql"));
        initializer.setDatabasePopulator(schemaPopulator);

        if (properties.isLoadSamples()) {
            insertSampleData(connectionFactory);
        }

        return initializer;
    }

    private void insertSampleData(ConnectionFactory connectionFactory) {
        DatabaseClient client = DatabaseClient.create(connectionFactory);

        // --- Define concurrent tasks ---
        Mono<Void> chatSessionCheck = populateSampleDataIfEmpty(
                client, connectionFactory,
                "chat_session",
                "db/_sample/_chat_session.sql"
        );

        Mono<Void> chatMessageCheck = populateSampleDataIfEmpty(
                client, connectionFactory,
                "chat_message",
                "db/_sample/_chat_message.sql"
        );

        Mono<Void> auditCheck = populateSampleDataIfEmpty(
                client, connectionFactory,
                "ai_interaction_audit",
                "db/_sample/_ai_interaction_audit.sql"
        );

        // --- Run all concurrently ---
        Flux.merge(chatSessionCheck, chatMessageCheck, auditCheck)
                .doOnComplete(() -> log.info("Database initialization finished."))
                .doOnError(e -> log.error("Error during database initialization", e))
                .subscribe();
    }

    /**
     * Conditionally populates a table if it's empty.
     */
    private Mono<Void> populateSampleDataIfEmpty(
            DatabaseClient client, ConnectionFactory connectionFactory, String tableName, String scriptPath) {
        return client.sql("SELECT COUNT(*) AS cnt FROM " + tableName)
                .map(row -> row.get("cnt", Long.class))
                .first()
                .defaultIfEmpty(0L)
                .flatMap(count -> {
                    if (count == 0) {
                        log.info("No existing data found in '{}'. Populating sample data...", tableName);
                        ResourceDatabasePopulator populator =
                                new ResourceDatabasePopulator(new ClassPathResource(scriptPath));
                        return Mono.fromRunnable(() -> populator.populate(connectionFactory).block())
                                .doOnSuccess(v ->
                                        log.info("Sample data inserted into '{}'.", tableName))
                                .doOnError(e ->
                                        log.error("Failed to populate sample data for '{}'.", tableName, e))
                                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                                .then();
                    } else {
                        log.info("Table '{}' already has data - skipping population.", tableName);
                        return Mono.empty();
                    }
                });
    }
}