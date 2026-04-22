package com.bbmovie.auth.unit.security;

import com.bbmovie.auth.security.SecurityConfig;
import com.bbmovie.auth.security.jose.filter.JoseAuthenticationFilter;
import com.bbmovie.auth.security.oauth2.CustomAuthorizationRequestResolver;
import com.bbmovie.auth.security.oauth2.OAuth2LoginSuccessHandler;
import com.bbmovie.auth.service.auth.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class SecurityConfigTest {

    @Mock
    JoseAuthenticationFilter joseAuthenticationFilter;
    @Mock
    CorsConfigurationSource corsConfigurationSource;
    @Mock
    CustomUserDetailsService userDetailsService;
    @Mock
    OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    @Mock
    CustomAuthorizationRequestResolver customAuthorizationRequestResolver;

    @Test
    void passwordEncoder_ShouldReturnDelegatingPasswordEncoder() {
        // When
        PasswordEncoder passwordEncoder = new SecurityConfig(
                joseAuthenticationFilter,
                corsConfigurationSource,
                userDetailsService,
                oAuth2LoginSuccessHandler,
                customAuthorizationRequestResolver
        ).passwordEncoder();

        // Then
        assertNotNull(passwordEncoder);
        assertTrue(passwordEncoder.getClass().getSimpleName().contains("DelegatingPasswordEncoder"));
    }

    @Test
    void getActiveEncodersName_ShouldReturnCorrectEncoders() {
        // When
        var activeEncoders = SecurityConfig.getActiveEncodersName();

        // Then
        assertNotNull(activeEncoders);
        assertTrue(activeEncoders.contains("bcrypt"));
        assertTrue(activeEncoders.contains("argon2"));
        assertTrue(activeEncoders.contains("pbkdf2"));
        assertEquals(3, activeEncoders.size());
    }

    @Test
    void getDeprecatedEncodersName_ShouldReturnCorrectEncoders() {
        // When
        var deprecatedEncoders = SecurityConfig.getDeprecatedEncodersName();

        // Then
        assertNotNull(deprecatedEncoders);
        assertTrue(deprecatedEncoders.contains("noop"));
        assertTrue(deprecatedEncoders.contains("md4"));
    }

    @Test
    void wrapWithBraces_ShouldAddBraces() {
        // When
        String result = SecurityConfig.wrapWithBraces("test");

        // Then
        assertEquals("{test}", result);
    }

    @Test
    void wrapWithBraces_WhenAlreadyWrapped_ShouldNotAddBraces() {
        // When
        String result = SecurityConfig.wrapWithBraces("{test}");

        // Then
        assertEquals("{test}", result);
    }

    @Test
    void unwrapBraces_ShouldRemoveBraces() {
        // When
        String result = SecurityConfig.unwrapBraces("{test}");

        // Then
        assertEquals("test", result);
    }

    @Test
    void unwrapBraces_WhenNotWrapped_ShouldReturnOriginal() {
        // When
        String result = SecurityConfig.unwrapBraces("test");

        // Then
        assertEquals("test", result);
    }

    @Test
    void isDeprecatedHash_ShouldReturnTrueForDeprecatedPrefix() {
        // When
        boolean result = SecurityConfig.isDeprecatedHash("{noop}somehash");

        // Then
        assertTrue(result);
    }

    @Test
    void isDeprecatedHash_ShouldReturnFalseForActivePrefix() {
        // When
        boolean result = SecurityConfig.isDeprecatedHash("{bcrypt}somehash");

        // Then
        assertFalse(result);
    }

    @Test
    void isActiveHash_ShouldReturnTrueForActivePrefix() {
        // When
        boolean result = SecurityConfig.isActiveHash("{bcrypt}somehash");

        // Then
        assertTrue(result);
    }

    @Test
    void isActiveHash_ShouldReturnFalseForDeprecatedPrefix() {
        // When
        boolean result = SecurityConfig.isActiveHash("{noop}somehash");

        // Then
        assertFalse(result);
    }
}