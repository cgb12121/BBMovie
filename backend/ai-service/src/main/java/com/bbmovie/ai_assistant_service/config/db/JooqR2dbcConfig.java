package com.bbmovie.ai_assistant_service.config.db;

import io.r2dbc.spi.ConnectionFactory;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.tools.LoggerListener;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqR2dbcConfig {
    @Bean
    public DSLContext dslContext(@Qualifier("connectionFactory") ConnectionFactory connectionFactory) {
        DefaultConfiguration configuration = new DefaultConfiguration();
        configuration.set(connectionFactory);
        configuration.set(SQLDialect.MYSQL);
        configuration.set(new LoggerListener());

        return DSL.using(configuration);
    }
}
