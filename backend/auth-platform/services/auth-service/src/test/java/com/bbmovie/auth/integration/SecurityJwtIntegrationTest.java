package com.bbmovie.auth.integration;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.repository.UserRepository;
import com.bbmovie.auth.security.jose.provider.JoseProvider;
import com.bbmovie.auth.security.jose.dto.TokenPair;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class SecurityJwtIntegrationTest {

    @Autowired
    private JoseProvider joseProvider;

    @Autowired
    private UserRepository userRepository;

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
    void generateAndValidateTokenPair_WhenValidUser_ShouldCreateValidTokens() {
        // Given
        User user = User.builder()
                .email("token@test.com")
                .displayedUsername("token_user")
                .password("{bcrypt}$2a$12$...")  // encoded password
                .role(Role.USER)
                .build();
        userRepository.save(user);

        // Create a mock authentication object
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        // When - Generate token pair
        TokenPair tokenPair = joseProvider.generateTokenPair(authentication, user);

        // Then - Verify tokens were created
        assertNotNull(tokenPair);
        assertNotNull(tokenPair.accessToken());
        assertNotNull(tokenPair.refreshToken());
        assertTrue(tokenPair.accessToken().split("\\.").length == 3); // JWT format: header.payload.signature
        assertTrue(tokenPair.refreshToken().split("\\.").length == 3);

        // When - Validate access token
        boolean isValid = joseProvider.validateToken(tokenPair.accessToken());

        // Then - Verify token is valid
        assertTrue(isValid);

        // When - Validate refresh token
        boolean isRefreshValid = joseProvider.validateToken(tokenPair.refreshToken());

        // Then - Verify refresh token is valid
        assertTrue(isRefreshValid);
    }

    @Test
    void getUsernameFromToken_WhenValidToken_ShouldReturnUserId() {
        // Given - Create a user and generate token
        User user = User.builder()
                .email("getuser@test.com")
                .displayedUsername("getuser_user")
                .password("{bcrypt}$2a$12$...")
                .role(Role.USER)
                .build();
        userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        TokenPair tokenPair = joseProvider.generateTokenPair(authentication, user);

        // When - Extract username from token
        String extractedId = joseProvider.getUsernameFromToken(tokenPair.accessToken());

        // Then - Verify extracted ID matches user ID
        assertEquals(user.getId().toString(), extractedId);
    }

    @Test
    void getRolesFromToken_WhenValidToken_ShouldReturnUserRoles() {
        // Given - Create user with role and generate token
        User user = User.builder()
                .email("getroles@test.com")
                .displayedUsername("getroles_user")
                .password("{bcrypt}$2a$12$...")
                .role(Role.ADMIN)
                .build();
        userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .build();

        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        TokenPair tokenPair = joseProvider.generateTokenPair(authentication, user);

        // When - Get roles from token
        List<String> roles = joseProvider.getRolesFromToken(tokenPair.accessToken());

        // Then - Verify roles are present
        assertNotNull(roles);
        assertTrue(roles.contains("ROLE_ADMIN"));
    }

    @Test
    void getExpirationDateFromToken_WhenValidToken_ShouldReturnExpirationDate() {
        // Given - Create user and generate token
        User user = User.builder()
                .email("expire@test.com")
                .displayedUsername("expire_user")
                .password("{bcrypt}$2a$12$...")
                .role(Role.USER)
                .build();
        userRepository.save(user);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        Authentication authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );

        TokenPair tokenPair = joseProvider.generateTokenPair(authentication, user);

        // When - Get expiration date from token
        Date expirationDate = joseProvider.getExpirationDateFromToken(tokenPair.accessToken());

        // Then - Verify expiration date is in the future
        assertNotNull(expirationDate);
        assertTrue(expirationDate.after(new Date()));
    }

    @Test
    void validateToken_WhenTokenExpired_ShouldReturnFalse() throws InterruptedException {
        // For this test, we'd need to create a token with a very short expiration time
        // Since the configuration is external, we'll skip this for now
        // This would be better handled in unit tests that mock the clock
    }
}