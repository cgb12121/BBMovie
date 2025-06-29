package com.example.bbmovieragrecommendation.service.llm;

import reactor.core.publisher.Mono;

public interface LLMClient {
    Mono<String> complete(String prompt);
}