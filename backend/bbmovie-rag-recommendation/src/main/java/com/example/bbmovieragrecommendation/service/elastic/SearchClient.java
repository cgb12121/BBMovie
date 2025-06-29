package com.example.bbmovieragrecommendation.service.elastic;

import com.example.bbmovieragrecommendation.dto.ContextSnippet;
import reactor.core.publisher.Flux;

public interface SearchClient {
    Flux<ContextSnippet> retrieve(String query, int topK);
}
