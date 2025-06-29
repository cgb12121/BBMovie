package com.example.bbmovieragrecommendation.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class MiniLLMClient implements LLMClient {
    private final WebClient client = WebClient.create("http://localhost:11434"); // ollama server

    public Mono<String> complete(String prompt) {
        return client.post()
            .uri("/v1/completions")
            .bodyValue(Map.of(
                "prompt", prompt,
                "max_tokens", 150,
                "temperature", 0.8
            ))
            .retrieve()
            .bodyToMono(JsonNode.class)
            .map(json -> json.path("choices").get(0).path("text").asText());
    }
}