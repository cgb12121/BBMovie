package com.example.bbmoviesearch.service.embedding;

import java.util.List;

interface EmbeddingService {
    float[] generateEmbedding(String text);

    float[][] generateEmbeddings(List<String> texts);
}