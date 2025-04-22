package com.example.bbmovie.service.embedding;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class HuggingFaceEmbeddingService {

    @Value("${huggingface.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    private static final String MODEL_URL = "https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2";
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 5000;

    public float[] generateEmbedding(String text) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<String>> request = new HttpEntity<>(Collections.singletonList(text), headers);

        ResponseEntity<List> response = null;
        int attempt = 1;

        while (attempt <= MAX_ATTEMPTS) {
            try {
                log.info("Sending request {} to Hugging Face API (attempt {}/{})", request.getBody(), attempt, MAX_ATTEMPTS);
                response = restTemplate.postForEntity(MODEL_URL, request, List.class);
                log.info("Received response from Hugging Face API {}", response);

                if (response.getStatusCode() == HttpStatus.OK) {
                    break;
                } else {
                    throw new HttpServerErrorException(response.getStatusCode(), "Unexpected status code");
                }
            } catch (HttpServerErrorException e) {
                if (e.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE && attempt < MAX_ATTEMPTS) {
                    long backoff = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                    log.warn("Received 503 Service Unavailable, retrying after {}ms (attempt {}/{})", backoff, attempt, MAX_ATTEMPTS);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        log.error("Thread interrupted during backoff", ie);
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                    attempt++;
                } else {
                    throw e;
                }
            }
        }

        if (response == null || response.getBody() == null) {
            throw new RuntimeException("Failed to get embedding from HuggingFace: No response");
        }

        List<?> result = (List<?>) response.getBody().get(0);
        float[] embedding = new float[result.size()];
        for (int i = 0; i < result.size(); i++) {
            embedding[i] = ((Number) result.get(i)).floatValue();
        }
        log.info("Embedding {}", embedding);
        return embedding;
    }
}