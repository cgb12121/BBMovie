package com.example.bbmovieragrecommendation.controller;

import com.example.bbmovieragrecommendation.dto.RAGResponse;
import com.example.bbmovieragrecommendation.service.rag.RAGService;
import com.example.bbmovieragrecommendation.dto.RAGQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rag")
public class RAGController {
    private final RAGService ragService;

    @PostMapping("/recommend")
    public Mono<ResponseEntity<RAGResponse>> recommend(@RequestBody RAGQuery query) {
        return ragService.generateAnswer(query)
                .map(ResponseEntity::ok);
    }
}