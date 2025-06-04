package com.example.bbmovie.security;

public class EndPointsConfig {

    protected EndPointsConfig() {}

    protected static final String[] AUTH_ENDPOINTS = {
            "/api/auth/**",
            "/api/device/**"
    };

    protected static final String[] SWAGGER_ENDPOINTS = {
            "/v3/api-docs",
            "/swagger-ui.html",
            "/swagger-resources/configuration/ui",
            "/v3/api-docs.yaml",
            "/v3/api-docs/swagger-config",
    };

    protected static final String[] ERRORS_ENDPOINTS = {
            "/error"
    };

    protected static final String[] SPRING_ACTUAL_ENDPOINTS = {
            "/actuator/**",
            "/actuator/health",
            "/actuator/info",
            "/actuator/prometheus",
            "/actuator/health/**",
    };
}
