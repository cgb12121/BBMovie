package com.bbmovie.ai_assistant_service.core.low_level._config._db;

import com.bbmovie.ai_assistant_service.core.low_level._config._db._converter.StringToUuidConverter;
import com.bbmovie.ai_assistant_service.core.low_level._config._db._converter.UuidToStringConverter;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.extern.slf4j.Slf4j;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@EnableR2dbcRepositories(
        basePackages = "com.bbmovie.ai_assistant_service.core.low_level._repository",
        entityOperationsRef = "_EntityOperations"
)
@EnableR2dbcAuditing
public class _DatabaseConfig {

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
        return ConnectionFactories.get(options);
    }

    @Bean
    @Qualifier("_CustomConversions")
    public R2dbcCustomConversions _CustomConversions() {
        List<Object> converters = new ArrayList<>();
        converters.add(UuidToStringConverter.INSTANCE);
        converters.add(StringToUuidConverter.INSTANCE);
        return new R2dbcCustomConversions(R2dbcCustomConversions.StoreConversions.NONE, converters);
    }

    @Bean
    @Qualifier("_R2dbcConverter")
    public R2dbcConverter _R2dbcConverter(
            @Qualifier("_CustomConversions") R2dbcCustomConversions customConversions) {
        R2dbcMappingContext mappingContext = new R2dbcMappingContext();
        mappingContext.afterPropertiesSet();
        return new MappingR2dbcConverter(mappingContext, customConversions);
    }

    @Bean
    @Qualifier("_DatabaseClient")
    public DatabaseClient _DatabaseClient(@Qualifier("_ConnectionFactory") ConnectionFactory connectionFactory) {
        return DatabaseClient.create(connectionFactory);
    }

    @Bean
    @Qualifier("_EntityOperations")
    public R2dbcEntityOperations _EntityOperations(
            @Qualifier("_DatabaseClient") DatabaseClient databaseClient,
            @Qualifier("_ConnectionFactory") ConnectionFactory connectionFactory,
            @Qualifier("_R2dbcConverter") R2dbcConverter r2dbcConverter) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        return new R2dbcEntityTemplate(databaseClient, dialect, r2dbcConverter);
    }

    @Bean
    @Qualifier("_TransactionManager")
    public ReactiveTransactionManager _TransactionManager(
            @Qualifier("_ConnectionFactory") ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }

    @Bean
    @Qualifier("_DbInitializer")
    public ConnectionFactoryInitializer _DbInitializerWithSchemaValidation(
            @Qualifier("_ConnectionFactory") ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator schemaPopulator = new ResourceDatabasePopulator();
        schemaPopulator.addScript(new ClassPathResource("db/_schema.sql"));
        initializer.setDatabasePopulator(schemaPopulator);

        try {
            ResourceDatabasePopulator dataPopulator = new ResourceDatabasePopulator();
            dataPopulator.addScript(new ClassPathResource("db/_data.sql"));
            initializer.setDatabasePopulator(dataPopulator);
        } catch (Exception e) {
            log.warn("Data population failed, but schema initialization succeeded.", e);
        }

        return initializer;
    }
}
