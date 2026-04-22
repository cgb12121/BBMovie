package com.bbmovie.auth.integration;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.entity.enumerate.AuthProvider;
import com.bbmovie.auth.entity.enumerate.Role;
import com.bbmovie.auth.repository.UserRepository;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.yml")
class UserRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

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
    void saveUser_WhenValidUser_ShouldPersistToDatabase() {
        // Given
        User user = User.builder()
                .email("integration@test.com")
                .displayedUsername("integration_user")
                .password("{bcrypt}$2a$12$...")  // Encoded password
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .isEnabled(true)
                .build();

        // When
        User savedUser = userRepository.save(user);

        // Then
        assertNotNull(savedUser.getId());
        assertEquals("integration@test.com", savedUser.getEmail());
        assertEquals("integration_user", savedUser.getDisplayedUsername());
        assertTrue(savedUser.isEnabled());
        assertEquals(Role.USER, savedUser.getRole());
        assertEquals(AuthProvider.LOCAL, savedUser.getAuthProvider());
    }

    @Test
    void findByEmail_WhenUserExists_ShouldReturnUser() {
        // Given - Create and persist user
        User user = User.builder()
                .email("findbyemail@test.com")
                .displayedUsername("findbyemail_user")
                .password("{bcrypt}$2a$12$...")
                .role(Role.USER)
                .isEnabled(true)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // When
        Optional<User> foundUser = userRepository.findByEmail("findbyemail@test.com");

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals("findbyemail@test.com", foundUser.get().getEmail());
        assertEquals("findbyemail_user", foundUser.get().getDisplayedUsername());
    }

    @Test
    void findByDisplayedUsername_WhenUserExists_ShouldReturnUser() {
        // Given
        User user = User.builder()
                .email("username@test.com")
                .displayedUsername("unique_username")
                .password("{bcrypt}$2a$12$...")
                .role(Role.USER)
                .isEnabled(true)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // When
        Optional<User> foundUser = userRepository.findByDisplayedUsername("unique_username");

        // Then
        assertTrue(foundUser.isPresent());
        assertEquals("unique_username", foundUser.get().getDisplayedUsername());
        assertEquals("username@test.com", foundUser.get().getEmail());
    }

    @Test
    void existsByEmail_WhenEmailExists_ShouldReturnTrue() {
        // Given
        User user = User.builder()
                .email("exists@test.com")
                .displayedUsername("exists_user")
                .password("{bcrypt}$2a$12$...")
                .role(Role.USER)
                .isEnabled(true)
                .build();
        entityManager.persist(user);
        entityManager.flush();

        // When
        boolean exists = userRepository.existsByEmail("exists@test.com");

        // Then
        assertTrue(exists);
    }

    @Test
    void existsByEmail_WhenEmailDoesNotExist_ShouldReturnFalse() {
        // When
        boolean exists = userRepository.existsByEmail("nonexistent@test.com");

        // Then
        assertFalse(exists);
    }
}