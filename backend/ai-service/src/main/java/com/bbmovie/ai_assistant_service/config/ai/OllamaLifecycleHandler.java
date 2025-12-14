package com.bbmovie.ai_assistant_service.config.ai;

import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component("ollamaLifecycleHandler")
public class OllamaLifecycleHandler {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(OllamaLifecycleHandler.class);

    private final WebClient webClient;
    private final ModelSelector aiSelector;

    @Autowired
    public OllamaLifecycleHandler(
            @Value("${ai.ollama.url}") String ollamaUrl,
            ModelSelector aiSelector) {
        this.aiSelector = aiSelector;

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60));

        this.webClient = WebClient.builder()
                .baseUrl(ollamaUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpOllama() {
        String modelName = aiSelector.getModelName();
        log.info("[Async] Triggering warmup (Load & Keep-Alive) for Ollama model: {}", modelName);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("prompt", ""); 
        body.put("keep_alive", -1); // Keep the model in VRAM 4ver => considering shorten the TTL on cloud

        webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                        .doBeforeRetry(s -> log.warn("Ollama warmup retry: {}...", s.totalRetriesInARow() + 1))
                )
                .subscribe(
                        response -> log.info("Ollama Model [{}] Warmed up & Ready!", modelName),
                        error -> log.error("Ollama Warmup Failed [{}]. Error: {}", modelName, error.getMessage())
                );
    }

    @PreDestroy
    public void unloadOllama() {
        String modelName = aiSelector.getModelName();
        log.info("Application shutting down... Unloading Ollama model: {}", modelName);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("keep_alive", 0); // Unload model immediately

        try {
            // ⚠️ NOTE:
            // When shutdown, must .block() (Synchronous)
            // If use .subscribe() (Async), JVM might be turned off before the request finish
            webClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(10));

            log.info("Ollama Model [{}] Unloaded successfully. VRAM freed.", modelName);
        } catch (Exception e) {
            log.warn("Failed to unload model [{}] during shutdown (Maybe Ollama is already down?). Error: {}", modelName, e.getMessage());
        }
    }
}