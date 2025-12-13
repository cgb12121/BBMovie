package com.bbmovie.ai_assistant_service.config.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
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

@Slf4j
@Component("ollamaWarmupHandler")
public class OllamaWarmupHandler {

    private final WebClient webClient;
    private final ModelSelector aiSelector;
    private final ConfigurableApplicationContext context;

    @Autowired
    public OllamaWarmupHandler(
            @Value("${ai.ollama.url}") String ollamaUrl,
            ModelSelector aiSelector,
            ConfigurableApplicationContext context) {
        this.aiSelector = aiSelector;
        this.context = context;

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
        log.info("[Async] Triggering warmup for Ollama model: {}", modelName);

        Map<String, Object> body = new HashMap<>();
        body.put("model", modelName);
        body.put("prompt", ""); // Empty prompt just to load model
        body.put("keep_alive", -1); // Keep in RAM forever

        webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                        .doBeforeRetry(s -> log.warn("Ollama is not ready, retry attempt: {}...", s.totalRetriesInARow() + 1))
                )
                .subscribe(
                        response -> {
                            log.info("Ollama loaded [{}] success! Self-destruction triggered.", modelName);
                            selfDestruct();
                        },
                        error -> {
                            log.error("Can not eager load the model right now [{}]. Error: {}", modelName, error.getMessage());
                            selfDestruct();
                        }
                );
    }

    private void selfDestruct() {
        try {
            var beanFactory = context.getBeanFactory();
            // Delete bean instance
            beanFactory.destroyBean(this);
            // (Optional) Can remove definition here
             ((BeanDefinitionRegistry) beanFactory).removeBeanDefinition("ollamaWarmupHandler");
        } catch (Exception e) {
            log.warn("Can not destroy bean {} (Ignore it). Error: {}", this.getClass().getName() , e.getMessage());
        }
    }
}
