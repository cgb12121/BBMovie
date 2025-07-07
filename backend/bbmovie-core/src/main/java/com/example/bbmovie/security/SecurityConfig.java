package com.example.bbmovie.security;

import com.example.bbmovie.security.anonymity.IpAnonymityFilter;
import com.example.bbmovie.security.jose.JoseFilter;
import com.example.bbmovie.security.oauth2.CustomAuthorizationRequestResolver;
import com.example.bbmovie.security.oauth2.OAuth2LoginSuccessHandler;
import com.example.bbmovie.service.auth.CustomUserDetailsService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JoseFilter joseFilter;
    private final IpAnonymityFilter ipAnonymityFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomUserDetailsService userDetailsService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;

    @Autowired
    public SecurityConfig(
            JoseFilter joseFilter, IpAnonymityFilter ipAnonymityFilter, CorsConfigurationSource corsConfigurationSource, CustomUserDetailsService userDetailsService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler, CustomAuthorizationRequestResolver customAuthorizationRequestResolver
    ) {
        this.joseFilter = joseFilter;
        this.ipAnonymityFilter = ipAnonymityFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.userDetailsService = userDetailsService;
        this.oAuth2LoginSuccessHandler = oAuth2LoginSuccessHandler;
        this.customAuthorizationRequestResolver = customAuthorizationRequestResolver;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .headers(header -> header
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("style-src 'self'; script-src 'self'; form-action 'self'; report-uri /report; report-to csp-violation-report")
                )
                .httpStrictTransportSecurity(sts -> sts
                        .maxAgeInSeconds(31536000)
                        .includeSubDomains(true)
                        .preload(true)
                )
                .xssProtection(xss -> xss
                        .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED)
                )
                .contentTypeOptions(HeadersConfigurer.ContentTypeOptionsConfig::disable)
                .referrerPolicy(referrer -> referrer
                        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)
                )
                .permissionsPolicyHeader(policy -> policy
                        .policy("geolocation=(self), camera=(), microphone=()")
                )
                .cacheControl(HeadersConfigurer.CacheControlConfig::disable)
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/.well-known/jwks.json")
                .ignoringRequestMatchers("/ws/**")
                .ignoringRequestMatchers("/api/auth/csrf")
                .ignoringRequestMatchers("/oauth2/authorization/**")
                .ignoringRequestMatchers("/actuator/**")
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
            )
            .cors(cors -> cors
                .configurationSource(corsConfigurationSource)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(EndPointsConfig.AUTH_ENDPOINTS).permitAll()
                .requestMatchers(EndPointsConfig.SPRING_ACTUAL_ENDPOINTS).permitAll() //TODO: need to secure actuator, expose for testing purpose only
                .requestMatchers(EndPointsConfig.ERRORS_ENDPOINTS).permitAll()
                .requestMatchers(EndPointsConfig.SWAGGER_ENDPOINTS).permitAll()
                .requestMatchers("/api/cloudinary/**").hasRole("ADMIN")
                .requestMatchers("/api/search/**").permitAll()
                .requestMatchers("/.well-known/jwks.json").permitAll()
                // Allow websocket handshake, jwt will be processed later in interceptor
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write("Unauthorized request");
//                            response.sendRedirect("http://localhost:3000/unauthorized");
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
            .addFilterBefore(joseFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(ipAnonymityFilter, JoseFilter.class)
            .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
        daoAuthenticationProvider.setUserDetailsService(userDetailsService);
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