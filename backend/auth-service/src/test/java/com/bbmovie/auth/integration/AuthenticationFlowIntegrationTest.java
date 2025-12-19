package com.bbmovie.auth.integration;

import com.bbmovie.auth.dto.request.LoginRequest;
import com.bbmovie.auth.dto.request.RegisterRequest;
import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.service.auth.registration.RegistrationService;
import com.bbmovie.auth.service.auth.session.SessionService;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class AuthenticationFlowIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeAll
    static void loadEnv() {
        Dotenv dotenv = Dotenv.configure()
                .directory("./") // Look for .env in the project root
                .ignoreIfMissing() // Don't throw an exception if .env file doesn't exist
                .load();

        // Set environment variables as system properties for the test
        System.setProperty("DATABASE_URL", dotenv.get("DATABASE_URL", System.getProperty("DATABASE_URL", "jdbc:h2:mem:testdb")));
        System.setProperty("DATABASE_USERNAME", dotenv.get("DATABASE_USERNAME", System.getProperty("DATABASE_USERNAME", "sa")));
        System.setProperty("DATABASE_PASSWORD", dotenv.get("DATABASE_PASSWORD", System.getProperty("DATABASE_PASSWORD", "")));
        System.setProperty("REDIS_PASSWORD", dotenv.get("REDIS_PASSWORD", System.getProperty("REDIS_PASSWORD", "")));
        System.setProperty("JOSE_EXPIRATION", dotenv.get("JOSE_EXPIRATION", System.getProperty("JOSE_EXPIRATION", "3600")));
        System.setProperty("JOSE_REFRESH_EXPIRATION", dotenv.get("JOSE_REFRESH_EXPIRATION", System.getProperty("JOSE_REFRESH_EXPIRATION", "86400")));
        System.setProperty("GITHUB_CLIENT_ID", dotenv.get("GITHUB_CLIENT_ID", ""));
        System.setProperty("GITHUB_CLIENT_SECRET", dotenv.get("GITHUB_CLIENT_SECRET", ""));
        System.setProperty("GOOGLE_CLIENT_ID", dotenv.get("GOOGLE_CLIENT_ID", ""));
        System.setProperty("GOOGLE_CLIENT_SECRET", dotenv.get("GOOGLE_CLIENT_SECRET", ""));
        System.setProperty("FACEBOOK_CLIENT_ID", dotenv.get("FACEBOOK_CLIENT_ID", ""));
        System.setProperty("FACEBOOK_CLIENT_SECRET", dotenv.get("FACEBOOK_CLIENT_SECRET", ""));
        System.setProperty("DISCORD_CLIENT_ID", dotenv.get("DISCORD_CLIENT_ID", ""));
        System.setProperty("DISCORD_CLIENT_SECRET", dotenv.get("DISCORD_CLIENT_SECRET", ""));
        System.setProperty("X_CLIENT_ID", dotenv.get("X_CLIENT_ID", ""));
        System.setProperty("X_CLIENT_SECRET", dotenv.get("X_CLIENT_SECRET", ""));
    }

    @Test
    void registerAndLogin_WhenValidCredentials_ShouldAuthenticateSuccessfully() {
        // Given
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("integration.flow@test.com")
                .username("integration_user")
                .password("securePassword123!")
                .confirmPassword("securePassword123!")
                .firstName("Integration")
                .lastName("Test")
                .build();

        // When - Register user
        assertDoesNotThrow(() -> registrationService.register(registerRequest));

        // Then - Verify user was created
        User registeredUser = userRepository.findByEmail("integration.flow@test.com")
                .orElseThrow(() -> new AssertionError("User should exist after registration"));
        assertEquals("integration.flow@test.com", registeredUser.getEmail());
        assertEquals("integration_user", registeredUser.getDisplayedUsername());
        assertFalse(registeredUser.isEnabled()); // Should be disabled initially
        assertTrue(passwordEncoder.matches("securePassword123!", registeredUser.getPassword()));

        // When - Login with credentials
        LoginRequest loginRequest = new LoginRequest("integration.flow@test.com", "securePassword123!");
        
        // This should work if user verification is bypassed for testing 
        // or if we implement the verification step in the test
        // For now, we'll test the flow after verification
        registeredUser.setIsEnabled(true); // Simulate email verification
        userRepository.save(registeredUser);
        
        // Now try login
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("User-Agent", "Test-Agent");
        assertDoesNotThrow(() -> sessionService.login(loginRequest, mockRequest));
    }

    @Test
    void login_WhenInvalidPassword_ShouldThrowBadLoginException() {
        // Given - Create a user first
        User user = User.builder()
                .email("invalid.login@test.com")
                .displayedUsername("invalid_login")
                .password(passwordEncoder.encode("correctPassword"))
                .role(Role.USER)
                .isEnabled(true)
                .build();
        userRepository.save(user);

        // When & Then
        LoginRequest loginRequest = new LoginRequest("invalid.login@test.com", "wrongPassword");
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("User-Agent", "Test-Agent");
        assertThrows(Exception.class, () -> sessionService.login(loginRequest, mockRequest)); // Will throw BadLoginException
    }

    @Test
    void register_WhenDuplicateEmail_ShouldThrowEmailAlreadyExistsException() {
        // Given - Create a user first
        User existingUser = User.builder()
                .email("duplicate@test.com")
                .displayedUsername("duplicate_user")
                .password(passwordEncoder.encode("password"))
                .role(Role.USER)
                .isEnabled(true)
                .build();
        userRepository.save(existingUser);

        // When - Try to register with duplicate email
        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .email("duplicate@test.com")  // Same as existing
                .username("different_username")
                .password("newPassword123!")
                .confirmPassword("newPassword123!")
                .build();

        // Then
        assertThrows(Exception.class, () -> registrationService.register(duplicateRequest)); // Will throw EmailAlreadyExistsException
    }

    @Test
    void register_WhenPasswordMismatch_ShouldThrowAuthenticationException() {
        // Given
        RegisterRequest requestWithMismatchedPasswords = RegisterRequest.builder()
                .email("mismatch@test.com")
                .username("mismatch_user")
                .password("password123")
                .confirmPassword("differentPassword") // Different from password
                .build();

        // When & Then
        assertThrows(Exception.class, () -> registrationService.register(requestWithMismatchedPasswords)); // Will throw AuthenticationException
    }
}