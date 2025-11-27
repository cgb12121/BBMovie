package com.bbmovie.ai_assistant_service.config.db;

import io.r2dbc.spi.ConnectionFactory;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqR2dbcConfig {
    @Bean
    public DSLContext dslContext(@Qualifier("connectionFactory") ConnectionFactory connectionFactory) {
        return DSL.using(connectionFactory, SQLDialect.MYSQL);
    }
}
