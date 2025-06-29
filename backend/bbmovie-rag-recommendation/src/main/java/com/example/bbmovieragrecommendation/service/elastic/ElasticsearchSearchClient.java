package com.example.bbmovieragrecommendation.service.elastic;

import com.example.bbmovieragrecommendation.dto.ContextSnippet;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ElasticsearchSearchClient implements SearchClient {
    private final WebClient webClient;

    public Flux<ContextSnippet> retrieve(String query, int topK) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .host("localhost").port(8081)
                .path("/search/similar-search")
                .queryParam("query", query)
                .queryParam("limit", topK)
                .build())
            .retrieve()
            .bodyToFlux(JsonNode.class)
            .map(node -> new ContextSnippet(
                node.path("title").asText(),
                node.path("type").asText(),
                node.path("description").asText(),
                node.path("_score").asDouble()))
            ;
    }
}