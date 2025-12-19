package com.bbmovie.auth;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectPackages;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@SelectPackages("com.bbmovie.auth.integration")
@TestPropertySource(locations = "classpath:application-test.yml")
class AuthApplicationTests {

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
    void contextLoads() {
    }

}
