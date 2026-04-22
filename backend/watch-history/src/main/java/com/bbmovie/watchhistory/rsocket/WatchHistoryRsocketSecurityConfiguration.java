package com.bbmovie.watchhistory.rsocket;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.config.annotation.rsocket.RSocketSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.rsocket.core.PayloadSocketAcceptorInterceptor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Configuration
@EnableRSocketSecurity
public class WatchHistoryRsocketSecurityConfiguration {

    @Bean
    public PayloadSocketAcceptorInterceptor rsocketSecurityInterceptor(RSocketSecurity rsocket) {
        rsocket.authorizePayload(authorize -> authorize.anyRequest().authenticated().anyExchange().permitAll())
                .jwt(Customizer.withDefaults());
        return rsocket.build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(JwtDecoder jwtDecoder) {
        return token -> Mono.fromCallable(() -> jwtDecoder.decode(token)).subscribeOn(Schedulers.boundedElastic());
    }
}
