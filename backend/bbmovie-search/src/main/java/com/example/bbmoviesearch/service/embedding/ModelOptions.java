package com.example.bbmoviesearch.service.embedding;

public interface ModelOptions {
    String MONOLINGUAL_ENGLISH_ONLY_MODEL = "djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2";
    String MULTILINGUAL_MODEL = "djl://ai.djl.huggingface.pytorch/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2";
}
