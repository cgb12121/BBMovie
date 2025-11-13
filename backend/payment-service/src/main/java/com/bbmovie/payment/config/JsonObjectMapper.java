package com.bbmovie.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonObjectMapper {
    @Bean("paymentObjectMapper")
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper();
    }
}
