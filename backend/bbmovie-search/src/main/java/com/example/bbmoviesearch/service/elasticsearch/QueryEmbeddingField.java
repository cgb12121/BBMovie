package com.example.bbmoviesearch.service.elasticsearch;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryEmbeddingField {
    public static final String EMBEDDING_FIELD = "contentVector";
}
