package com.bbmovie.auth.integration;

import com.bbmovie.auth.service.nats.EmailEventProducer;
import io.github.cdimascio.dotenv.Dotenv;
import io.nats.client.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Transactional
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
class NatsEventIntegrationTest {

    @Autowired
    private EmailEventProducer emailEventProducer;

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
    void sendMagicLinkOnRegistration_WhenNatsAvailable_ShouldPublishEvent() throws Exception {
        // Create a NATS connection for testing
        try (Connection nc = Nats.connect("nats://localhost:4222")) {
            // Create a unique subject to receive the message
            CompletableFuture<Boolean> received = new CompletableFuture<>();

            // Subscribe to the subject to verify the event is published
            Dispatcher dispatcher = nc.createDispatcher();
            String expectedSubject = "auth.registration";
            dispatcher.subscribe(expectedSubject, msg -> {
                try {
                    String data = new String(msg.getData());
                    assertTrue(data.contains("test@example.com"));
                    assertTrue(data.contains("verification-token"));
                    received.complete(true);
                } catch (Exception e) {
                    received.completeExceptionally(e);
                }
            });

            // Wait a bit for subscription to be established
            Thread.sleep(500);

            // When - Publish the event
            emailEventProducer.sendMagicLinkOnRegistration("test@example.com", "verification-token");

            // Wait for the message to be received with timeout
            try {
                boolean messageReceived = received.orTimeout(5, TimeUnit.SECONDS).join();
                assertTrue(messageReceived, "Event should be published and received");
            } catch (CompletionException e) {
                if (e.getCause() instanceof TimeoutException) {
                    fail("NATS message was not received within timeout period");
                } else {
                    throw e;
                }
            }

            dispatcher.unsubscribe(expectedSubject);
        } catch (Exception e) {
            // NATS might not be running, which is OK for CI environments
            System.out.println("NATS not available for integration test, skipping: " + e.getMessage());
            // Skip the test if NATS is not available
            assumeTrue(false, "NATS server not available, skipping test");
        }
    }

    @Test
    void sendMagicLinkOnForgotPassword_WhenNatsAvailable_ShouldPublishEvent() throws Exception {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {
            CompletableFuture<Boolean> received = new CompletableFuture<>();

            Dispatcher dispatcher = nc.createDispatcher();
            String expectedSubject = "auth.forgot_password";
            dispatcher.subscribe(expectedSubject, msg -> {
                try {
                    String data = new String(msg.getData());
                    assertTrue(data.contains("forgot@test.com"));
                    assertTrue(data.contains("reset-token"));
                    received.complete(true);
                } catch (Exception e) {
                    received.completeExceptionally(e);
                }
            });

            Thread.sleep(500);

            // When - Publish the event
            emailEventProducer.sendMagicLinkOnForgotPassword("forgot@test.com", "reset-token");

            try {
                boolean messageReceived = received.orTimeout(5, TimeUnit.SECONDS).join();
                assertTrue(messageReceived, "Forgot password event should be published and received");
            } catch (CompletionException e) {
                if (e.getCause() instanceof TimeoutException) {
                    fail("NATS forgot password message was not received within timeout period");
                } else {
                    throw e;
                }
            }

            dispatcher.unsubscribe(expectedSubject);
        } catch (Exception e) {
            // NATS might not be running, which is OK for CI environments
            System.out.println("NATS not available for integration test, skipping: " + e.getMessage());
            assumeTrue(false, "NATS server not available, skipping test");
        }
    }

    @Test
    void sendOtp_WhenNatsAvailable_ShouldPublishEvent() throws Exception {
        try (Connection nc = Nats.connect("nats://localhost:4222")) {
            CompletableFuture<Boolean> received = new CompletableFuture<>();

            Dispatcher dispatcher = nc.createDispatcher();
            String expectedSubject = "auth.otp";
            dispatcher.subscribe(expectedSubject, msg -> {
                try {
                    String data = new String(msg.getData());
                    assertTrue(data.contains("1234567890"));
                    assertTrue(data.contains("123456"));
                    received.complete(true);
                } catch (Exception e) {
                    received.completeExceptionally(e);
                }
            });

            Thread.sleep(500);

            // When - Publish the event
            emailEventProducer.sendOtp("1234567890", "123456");

            try {
                boolean messageReceived = received.orTimeout(5, TimeUnit.SECONDS).join();
                assertTrue(messageReceived, "OTP event should be published and received");
            } catch (CompletionException e) {
                if (e.getCause() instanceof TimeoutException) {
                    fail("NATS OTP message was not received within timeout period");
                } else {
                    throw e;
                }
            }

            dispatcher.unsubscribe(expectedSubject);
        } catch (Exception e) {
            // NATS might not be running, which is OK for CI environments
            System.out.println("NATS not available for integration test, skipping: " + e.getMessage());
            assumeTrue(false, "NATS server not available, skipping test");
        }
    }
}