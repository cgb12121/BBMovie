package com.bbmovie.revenuedashboard.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class ClickHouseConfig {

    @Bean
    @ConditionalOnProperty(name = "revenue.analytics.clickhouse.enabled", havingValue = "true")
    public DataSource clickHouseDataSource(RevenueAnalyticsProperties properties) {
        var ch = properties.getClickhouse();
        var ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        ds.setUrl(ch.getJdbcUrl());
        ds.setUsername(ch.getUsername());
        ds.setPassword(ch.getPassword());
        return ds;
    }

    @Bean
    @ConditionalOnProperty(name = "revenue.analytics.clickhouse.enabled", havingValue = "true")
    public JdbcTemplate clickHouseJdbcTemplate(DataSource clickHouseDataSource) {
        return new JdbcTemplate(clickHouseDataSource);
    }
}
