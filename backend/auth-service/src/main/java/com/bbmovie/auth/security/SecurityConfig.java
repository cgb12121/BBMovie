package com.bbmovie.auth.security;

import com.bbmovie.auth.security.jose.filter.JoseAuthenticationFilter;
import com.bbmovie.auth.security.oauth2.CustomAuthorizationRequestResolver;
import com.bbmovie.auth.security.oauth2.OAuth2LoginSuccessHandler;
import com.bbmovie.auth.service.auth.CustomUserDetailsService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final CustomUserDetailsService userDetailsService;
    private final JoseAuthenticationFilter joseAuthenticationFilter;
    private final CustomAuthorizationRequestResolver customAuthorizationRequestResolver;
    private final CorsConfigurationSource corsConfigurationSource;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Autowired
    public SecurityConfig(
            JoseAuthenticationFilter joseAuthenticationFilter,
            CorsConfigurationSource corsConfigurationSource,
            CustomUserDetailsService userDetailsService,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler,
            CustomAuthorizationRequestResolver customAuthorizationRequestResolver
    ) {
        this.userDetailsService = userDetailsService;
        this.joseAuthenticationFilter = joseAuthenticationFilter;
        this.customAuthorizationRequestResolver = customAuthorizationRequestResolver;
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
                .disable() // Disable for stateless API using JWT, which doesn't rely on cookie authentication
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
                                frontendUrl + "/login?status=error&message=" + URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8)
                        );
                    })
                    .authorizationEndpoint(authorization -> authorization
                            .authorizationRequestResolver(customAuthorizationRequestResolver)
                    )
            )
            .addFilterBefore(joseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
//            .addFilterBefore(ipAnonymityFilter, JoseAuthenticationFilter.class) //the gateway will handle this
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

    public static final Map<String, PasswordEncoder> ACTIVE_ENCODER = Map.of(
            "bcrypt", new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2A, 12),
            "argon2", new Argon2PasswordEncoder(16, 32, 1, 1 << 14, 2),
            "pbkdf2", new Pbkdf2PasswordEncoder("", 16, 310000, PBKDF2WithHmacSHA256)
    );

    @SuppressWarnings("deprecation")
    public static final Map<String, PasswordEncoder> DEPRECATED_ENCODER = Map.of(
            "noop", NoOpPasswordEncoder.getInstance(),
            "md4", new Md4PasswordEncoder()
    );

    public static List<String> getActiveEncodersName() {
        return List.copyOf(ACTIVE_ENCODER.keySet());
    }

    public static List<String> getDeprecatedEncodersName() {
        return List.copyOf(DEPRECATED_ENCODER.keySet());
    }

    /**  around encoder names **/
    public static List<String> getActiveEncoderPrefixes() {
        return getActiveEncodersName().stream()
                .map(SecurityConfig::wrapWithBraces)
                .toList();
    }

    /**  for deprecated encoders **/
    public static List<String> getDeprecatedEncoderPrefixes() {
        return getDeprecatedEncodersName().stream()
                .map(SecurityConfig::wrapWithBraces)
                .toList();
    }

    /**  wrap with {} if not already wrapped **/
    public static String wrapWithBraces(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.startsWith("{") && name.endsWith("}")) return name;
        return "{" + name + "}";
    }

    /** remove {} if present **/
    public static String unwrapBraces(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.startsWith("{") && name.contains("}")) {
            return name.substring(1, name.indexOf('}'));
        }
        return name;
    }

    /** detect if hash uses deprecated encoder **/
    public static boolean isDeprecatedHash(String encoded) {
        if (encoded == null) return false;
        return getDeprecatedEncoderPrefixes().stream().anyMatch(encoded::startsWith);
    }

    /** detect if hash uses active encoder **/
    public static boolean isActiveHash(String encoded) {
        if (encoded == null) return false;
        return getActiveEncoderPrefixes().stream().anyMatch(encoded::startsWith);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        String defaultId = "argon2";
        Map<String, PasswordEncoder> encoders = new HashMap<>(ACTIVE_ENCODER);
        return new DelegatingPasswordEncoder(defaultId, encoders);
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