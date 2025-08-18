package com.bbmovie.watchlist.security

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class Security {

    @Value("\${app.auth.jwk}")
    private lateinit var jwkUri: String;

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .authorizeHttpRequests {
                it.requestMatchers("/test/hello").permitAll()
                    .requestMatchers("/test/secure").hasAuthority("SCOPE_message:read")
                    .requestMatchers("/test/secure").hasAnyRole("USER", "ADMIN")
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { obj -> obj.jwt(Customizer.withDefaults()) }
        return http.build()
    }


    @PostConstruct
    fun init() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)
    }

    @Bean
    fun jwkDecoder(): ReactiveJwtDecoder {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkUri).build()
    }
}