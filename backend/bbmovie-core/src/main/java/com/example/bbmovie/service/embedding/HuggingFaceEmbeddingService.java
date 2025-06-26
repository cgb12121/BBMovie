package com.example.bbmovie.service.embedding;

import com.example.bbmovie.exception.EmbeddingException;
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
public class HuggingFaceEmbeddingService implements EmbeddingService {

    @Value("${huggingface.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    private static final String MODEL_URL = "https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2";
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 5000;

    @SuppressWarnings("all")
    public float[] generateEmbedding(String text) {
        HttpHeaders headers = prepareHeaders();

        HttpEntity<List<String>> request = new HttpEntity<>(Collections.singletonList(text), headers);

        ResponseEntity<List> response = null;
        int attempt = 1;

        while (attempt <= MAX_ATTEMPTS) {
            try {
                response = restTemplate.postForEntity(MODEL_URL, request, List.class);

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
                        throw new EmbeddingException("Interrupted during retry backoff calling API for embedding.");
                    }
                    attempt++;
                } else {
                    throw e;
                }
            }
        }

        if (response == null || response.getBody() == null) {
            throw new EmbeddingException("Failed to getActiveProvider embedding from HuggingFace: No response");
        }

        List<?> result = (List<?>) response.getBody().get(0);
        float[] embedding = new float[result.size()];
        for (int i = 0; i < result.size(); i++) {
            embedding[i] = ((Number) result.get(i)).floatValue();
        }
        return embedding;
    }

    @Override
    @SuppressWarnings("all")
    public float[][] generateEmbeddings(List<String> texts) {
        HttpHeaders headers = prepareHeaders();

        HttpEntity<List<String>> request = new HttpEntity<>(texts, headers);

        ResponseEntity<List> response = null;
        int attempt = 1;

        while (attempt <= MAX_ATTEMPTS) {
            try {
                response = restTemplate.postForEntity(MODEL_URL, request, List.class);

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
                        throw new EmbeddingException("Interrupted during retry backoff calling API for embedding.");
                    }
                    attempt++;
                } else {
                    log.error("Failed to fetch embedding from HuggingFace", e);
                    throw new EmbeddingException("Failed to fetch embedding from HuggingFace");
                }
            }
        }

        if (response == null || response.getBody() == null) {
            throw new EmbeddingException("Failed to getActiveProvider embedding from HuggingFace: No response");
        }

        List<?> outerList = response.getBody();
        float[][] embeddings = new float[outerList.size()][];

        for (int i = 0; i < outerList.size(); i++) {
            List<?> innerList = (List<?>) outerList.get(i);
            embeddings[i] = new float[innerList.size()];
            for (int j = 0; j < innerList.size(); j++) {
                embeddings[i][j] = ((Number) innerList.get(j)).floatValue();
            }
        }

        return embeddings;
    }

    private HttpHeaders prepareHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}