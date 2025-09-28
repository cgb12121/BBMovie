package com.bbmovie.config;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private ScheduledExecutorService scheduler;
    private String instanceId;

    void onStart(@Observes StartupEvent ev) {
        this.instanceId = appName + "-" + UUID.randomUUID();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        register();

        scheduler.scheduleAtFixedRate(this::heartbeat, 30, 30, TimeUnit.SECONDS);
    }

    void onStop(@Observes ShutdownEvent ev) {
        deregister();
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                log.fatal("Scheduler did not terminate in 5 seconds");
                Thread.currentThread().interrupt();
            }
        }
    }

    private void register() {
        JsonObject instance = Json.createObjectBuilder()
                .add("hostName", "localhost")
                .add("app", appName)
                .add("ipAddr", "127.0.0.1")
                .add("vipAddress", appName.toLowerCase())
                .add("secureVipAddress", appName.toLowerCase())
                .add("status", "UP")
                .add("port", Json.createObjectBuilder()
                        .add("$", port)
                        .add("@enabled", "true"))
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

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eurekaUrl + appsPath + appName))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
               log.info("Registered " + instanceId + " with Eureka");
            } else {
                log.error("Failed to register: " + response.statusCode() + " - " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.fatal(e);
            Thread.currentThread().interrupt();
        }
    }

    private void heartbeat() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eurekaUrl + appsPath + appName + "/" + instanceId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Heartbeat failed (" + response.statusCode() + ") → re-registering...");
                register();
            }
        } catch (IOException| InterruptedException e) {
            log.fatal("Heartbeat error: " + e.getMessage());
            register();
            Thread.currentThread().interrupt();
        }
    }

    private void deregister() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(eurekaUrl + appsPath + appName + "/" + instanceId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Deregistered " + instanceId + " from Eureka");
            } else {
               log.error("Deregister failed: " + response.statusCode());
            }
        } catch (InterruptedException | IOException e) {
            log.fatal("Deregister error: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
