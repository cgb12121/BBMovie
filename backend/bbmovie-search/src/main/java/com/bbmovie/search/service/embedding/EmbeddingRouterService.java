package com.bbmovie.search.service.embedding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class EmbeddingRouterService {

     private final EmbeddingService primaryEmbeddingService;

     @Autowired
     public EmbeddingRouterService(List<EmbeddingService> allServices) {
         this.primaryEmbeddingService = allServices.stream()
                 .filter(s -> s instanceof DjLEmbeddingService)
                 .findFirst()
                 .orElse(allServices.isEmpty()
                         ? null
                         : allServices.getFirst()
                 );
     }

     public Mono<float[]> embed(String text) {
         if (primaryEmbeddingService != null) {
             return primaryEmbeddingService.generateEmbedding(text);
         }
         return Mono.error(new IllegalStateException("No embedding service available."));
     }
}