package com.bbmovie.mediaservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.context.annotation.Bean;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

     private CorsConfiguration getCorsConfiguration() {
          CorsConfiguration corsConfiguration = new CorsConfiguration();
          corsConfiguration.addAllowedOrigin("*");
          corsConfiguration.addAllowedHeader("*");
          corsConfiguration.addAllowedMethod("*");
          return corsConfiguration;
     }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(request -> getCorsConfiguration()))
        .authorizeHttpRequests(auth -> auth
            .anyRequest().permitAll()
        )
        .build();
    }
}