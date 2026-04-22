package com.bbmovie.movieanalyticsservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class ClickHouseConfig {

    @Bean
    @ConditionalOnProperty(name = "movie.analytics.clickhouse.enabled", havingValue = "true")
    public DataSource clickHouseDataSource(MovieAnalyticsProperties properties) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        ds.setUrl(properties.getClickhouse().getJdbcUrl());
        ds.setUsername(properties.getClickhouse().getUsername());
        ds.setPassword(properties.getClickhouse().getPassword());
        return ds;
    }

    @Bean
    @ConditionalOnProperty(name = "movie.analytics.clickhouse.enabled", havingValue = "true")
    public JdbcTemplate clickHouseJdbcTemplate(DataSource clickHouseDataSource) {
        return new JdbcTemplate(clickHouseDataSource);
    }
}

