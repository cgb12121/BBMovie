package com.bbmovie.config;

import com.bbmovie.exception.EurekaDeregistrationException;
import com.bbmovie.exception.EurekaHeartbeatException;
import com.bbmovie.exception.EurekaInstanceNotFoundException;
import com.bbmovie.exception.EurekaRegistrationException;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import org.eclipse.microprofile.faulttolerance.*;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.ShutdownEvent;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class EurekaClient {

    private static final Logger log = Logger.getLogger(EurekaClient.class);

    @ConfigProperty(name = "quarkus.eureka.url", defaultValue = "http://localhost:8761")
    String eurekaUrl;

    @ConfigProperty(name = "quarkus.application.name", defaultValue = "watchlist")
    String appName;

    @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080")
    int port;

    @ConfigProperty(name = "quarkus.eureka.apps-path", defaultValue = "/eureka/apps/")
    String appsPath;

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private ScheduledExecutorService scheduler;
    private String instanceId;
    private final AtomicBoolean isRegistered = new AtomicBoolean(false);
    private final AtomicBoolean isRegistering = new AtomicBoolean(false);

    void onStart(@Observes StartupEvent ev) {
        this.instanceId = appName + "-" + UUID.randomUUID();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "eureka-client-thread");
            t.setDaemon(true);
            return t;
        });

        // Register with automatic retries
        try {
            registerWithRetry();
            isRegistered.set(true);
            log.info("Successfully registered with Eureka");
        } catch (Exception e) {
            log.error("Failed to register with Eureka after all retry attempts: " + e.getMessage());
        }

        // Schedule heartbeat
        scheduler.scheduleAtFixedRate(this::heartbeatTask, 10, 5, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        deregisterTask();
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.error("Scheduler shutdown interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    @Retry(
            maxRetries = 5,
            delay = 2,
            delayUnit = ChronoUnit.SECONDS,
            maxDuration = 2,
            durationUnit = ChronoUnit.MINUTES,
            jitter = 500,
            jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {IOException.class, EurekaRegistrationException.class}
    )
    @Timeout(value = 15, unit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "registrationFallback")
    public void registerWithRetry() throws EurekaRegistrationException {
        // Prevent concurrent registration attempts
        if (!isRegistering.compareAndSet(false, true)) {
            log.debug("Registration already in progress, skipping");
            return;
        }

        try {
            register();
        } finally {
            isRegistering.set(false);
        }
    }

    private void register() throws EurekaRegistrationException {
        try {
            String ipAddress = getHostIpAddress();
            String hostName = getHostName();
            String homePageUrl = "http://" + ipAddress + ":" + port;
            String healthCheckUrl = homePageUrl + "/q/health";

            JsonObject instance = Json.createObjectBuilder()
                    .add("hostName", hostName)
                    .add("app", appName)
                    .add("ipAddr", ipAddress)
                    .add("vipAddress", appName.toLowerCase())
                    .add("secureVipAddress", appName.toLowerCase())
                    .add("status", "UP")
                    .add("port", Json.createObjectBuilder()
                            .add("$", port)
                            .add("@enabled", "true"))
                    .add("homePageUrl", homePageUrl)
                    .add("statusPageUrl", healthCheckUrl)
                    .add("healthCheckUrl", healthCheckUrl)
                    .add("dataCenterInfo", Json.createObjectBuilder()
                            .add("@class", "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo")
                            .add("name", "MyOwn"))
                    .add("instanceId", instanceId)
                    .add("metadata", Json.createObjectBuilder()
                            .add("version", "1.0")
                            .add("quarkus", "true")
                            .build())
                    .build();

            JsonObject payload = Json.createObjectBuilder()
                    .add("instance", instance)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eurekaUrl + appsPath + appName))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                log.info("Registered " + instanceId + " with Eureka");
            } else {
                log.error("Failed to register: " + response.statusCode() + " - " + response.body());
                throw new EurekaRegistrationException("Registration failed with status: " + response.statusCode());
            }
        } catch (IOException e) {
            log.error("IO error during registration: " + e.getMessage());
            throw new EurekaRegistrationException("IO error during registration", e);
        } catch (InterruptedException e) {
            log.error("Registration interrupted", e);
            Thread.currentThread().interrupt();
            throw new EurekaRegistrationException("Registration interrupted", e);
        }
    }

    private void heartbeatTask() {
        if (!isRegistered.get()) {
            log.debug("Not registered yet, skipping heartbeat");
            return;
        }

        try {
            sendHeartbeat();
            log.debug("Heartbeat successful");
        } catch (EurekaHeartbeatException e) {
            log.error("Heartbeat failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during heartbeat: " + e.getMessage());
        }
    }

    @Retry(
            maxRetries = 5,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS,
            jitter = 200,
            jitterDelayUnit = ChronoUnit.MILLIS,
            retryOn = {IOException.class},
            abortOn = {EurekaInstanceNotFoundException.class}
    )
    @CircuitBreaker(
            requestVolumeThreshold = 5,
            failureRatio = 1,
            delay = 30,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 2,
            failOn = {IOException.class, EurekaHeartbeatException.class}
    )
    @CircuitBreakerName("eureka-heartbeat")
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    @Fallback(fallbackMethod = "heartbeatFallback")
    public void sendHeartbeat() throws EurekaHeartbeatException, EurekaInstanceNotFoundException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eurekaUrl + appsPath + appName + "/" + instanceId))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Success
                log.debug("Connection is still alive");
            } else if (response.statusCode() == 404) {
                // Instance not found - need to re-register
                log.warn("Instance not found in Eureka (404), triggering re-registration...");
                isRegistered.set(false);
                throw new EurekaInstanceNotFoundException("Instance not found, re-registration needed");
            } else if (response.statusCode() >= 500) {
                // Server error - retry
                throw new EurekaHeartbeatException("Server error: " + response.statusCode());
            } else {
                // Client error - don't retry
                log.error("Heartbeat failed with client error: " + response.statusCode());
                throw new EurekaHeartbeatException("Client error: " + response.statusCode());
            }
        } catch (IOException e) {
            log.error("IO error during heartbeat: " + e.getMessage());
            throw new EurekaHeartbeatException("IO error during heartbeat", e);
        } catch (InterruptedException e) {
            log.error("Heartbeat interrupted", e);
            Thread.currentThread().interrupt();
            throw new EurekaHeartbeatException("Heartbeat interrupted", e);
        }
    }

    @SuppressWarnings("unused")
    private void heartbeatFallback(EurekaInstanceNotFoundException e) {
        log.warn("Heartbeat fallback: Instance not found, attempting re-registration...");
        isRegistered.set(false);
        try {
            registerWithRetry();
            isRegistered.set(true);
        } catch (Exception ex) {
            log.error("Re-registration failed in heartbeat fallback");
        }
    }

    @SuppressWarnings("unused")
    private void registrationFallback() {
        log.error("Registration fallback triggered - all retry attempts exhausted");
        isRegistered.set(false);
    }

    @SuppressWarnings("unused")
    private void heartbeatFallback(EurekaHeartbeatException e) {
        log.error("Heartbeat fallback triggered - circuit breaker may be open or all retries exhausted");
    }

    private void deregisterTask() {
        if (!isRegistered.get()) {
            log.debug("Not registered, skipping deregistration");
            return;
        }

        try {
            deregister();
            isRegistered.set(false);
        } catch (Exception e) {
            log.error("Deregistration failed: " + e.getMessage());
        }
    }

    @Retry(
            maxRetries = 5,
            delay = 1,
            delayUnit = ChronoUnit.SECONDS
    )
    @Timeout(value = 10, unit = ChronoUnit.SECONDS)
    public void deregister() throws EurekaDeregistrationException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eurekaUrl + appsPath + appName + "/" + instanceId))
                    .timeout(Duration.ofSeconds(10))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Deregistered " + instanceId + " from Eureka");
            } else {
                log.error("Deregister failed: " + response.statusCode());
                throw new EurekaDeregistrationException("Deregistration failed with status: " + response.statusCode());
            }
        } catch (IOException e) {
            log.error("IO error during deregistration: " + e.getMessage());
            throw new EurekaDeregistrationException("IO error during deregistration", e);
        } catch (InterruptedException e) {
            log.error("Deregister interrupted", e);
            Thread.currentThread().interrupt();
            throw new EurekaDeregistrationException("Deregistration interrupted", e);
        }
    }

    private String getHostIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Unable to determine host IP address, falling back to 127.0.0.1");
            return "127.0.0.1";
        }
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("Unable to determine hostname, falling back to localhost");
            return "localhost";
        }
    }
}