package com.bbmovie.auth.security;

import com.bbmovie.auth.security.anonymity.IpAnonymityFilter;
import com.bbmovie.auth.security.jose.JoseAuthenticationFilter;
import com.bbmovie.auth.security.oauth2.CustomAuthorizationRequestResolver;
import com.bbmovie.auth.security.oauth2.OAuth2LoginSuccessHandler;
import com.bbmovie.auth.service.auth.CustomUserDetailsService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JoseAuthenticationFilter joseAuthenticationFilter;
    private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;
    private final IpAnonymityFilter ipAnonymityFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Autowired
    public SecurityConfig(
            JoseAuthenticationFilter joseAuthenticationFilter, IpAnonymityFilter ipAnonymityFilter,
            CorsConfigurationSource corsConfigurationSource, CustomUserDetailsService userDetailsService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            CustomAuthorizationRequestResolver customAuthorizationRequestResolver
    ) {
        this.userDetailsService = userDetailsService;
        this.joseAuthenticationFilter = joseAuthenticationFilter;
        this.customAuthorizationRequestResolver = customAuthorizationRequestResolver;
        this.ipAnonymityFilter = ipAnonymityFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(header -> header
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .httpStrictTransportSecurity(sts -> sts
                        .maxAgeInSeconds(31536000)
                        .includeSubDomains(true)
                        .preload(true)
                )
                .cacheControl(HeadersConfigurer.CacheControlConfig::disable)
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/.well-known/jwks.json")
                .ignoringRequestMatchers("/auth/csrf")
                .ignoringRequestMatchers("/oauth2/authorization/**")
                .ignoringRequestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .cors(cors -> cors
                .configurationSource(corsConfigurationSource)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/.well-known/jwks.json").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/api/student-program/supported/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            log.warn("[AuthenticationEntryPoint] {}, message: {}",
                                    authException.getAuthenticationRequest(), authException.getLocalizedMessage()
                            );
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("Unauthorized request");
                        })
            )
            .requestCache(RequestCacheConfigurer::disable)
            .authenticationProvider(authenticationProvider())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .oauth2Login(oath2 -> oath2
                    .successHandler(oAuth2LoginSuccessHandler)
                    .failureHandler((request, response, exception) -> {
                        log.error(exception.getMessage(), exception);
                        response.sendRedirect(
                                "http://localhost:3000/login?status=error&message=" +
                                URLEncoder.encode("login via oauth2 failed", StandardCharsets.UTF_8)
                        );
                    })
                    .authorizationEndpoint(authorization -> authorization
                            .authorizationRequestResolver(customAuthorizationRequestResolver)
                    )
            )
            .addFilterBefore(joseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(ipAnonymityFilter, JoseAuthenticationFilter.class)
            .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        daoAuthenticationProvider.setPasswordEncoder(passwordEncoder());
        return daoAuthenticationProvider;
    }

    @PostConstruct
    public void init() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            this.xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
        }
    }   
}