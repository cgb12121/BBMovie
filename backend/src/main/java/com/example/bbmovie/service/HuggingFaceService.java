package com.example.bbmovie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
@Log4j2
@RequiredArgsConstructor
public class HuggingFaceService {

    @Value("${huggingface.api.key}")
    private String apiKey;

    private static final String MODEL_URL = "https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2";

    public float[] generateEmbedding(String text) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<List<String>> request = new HttpEntity<>(Collections.singletonList(text), headers);

        log.info("Sending request {} to huggingface api", request.getBody());
        ResponseEntity<List> response = restTemplate.postForEntity(MODEL_URL, request, List.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            List<?> result = (List<?>) response.getBody().get(0);
            float[] embedding = new float[result.size()];
            for (int i = 0; i < result.size(); i++) {
                embedding[i] = ((Number) result.get(i)).floatValue();
            }
            return embedding;
        } else {
            throw new RuntimeException("Failed to get embedding from HuggingFace: " + response.getStatusCode());
        }
    }
}
