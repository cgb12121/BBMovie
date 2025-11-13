package com.bbmovie.search.service.embedding;

import lombok.Getter;

@Getter
public enum DjLModelOptions {
    /**
     * Safe: Cross-model Embedding Compatibility
     * <p>
     * Mostly suitable when running both Ollama all-MiniLM and DJL all-MiniLM-L6-v2 (both 384 dims),
     * can expect very high cosine similarity (~0.95–0.99)
     */
    MONOLINGUAL_ENGLISH_ONLY_MODEL("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2", 384),

    /**
     * <b>Unsafe</b>:
     * <p>
     * Different model families with Ollama all-MiniLM (paraphrase-multilingual vs. all-MiniLM).
     * <p>
     * Mixing multilingual and monolingual models in the same Elasticsearch index —> vector search will return nonsense
     * because the cosine distance becomes meaningless. The same English sentence and embed it with both, cosine
     * similarities might be around 0.6–0.8
     */
    MULTILINGUAL_MODEL("djl://ai.djl.huggingface.pytorch/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2", 384);

    private final String modelUri;
    private final int dims;

    DjLModelOptions(String modelUri, int dims) {
        this.modelUri = modelUri;
        this.dims = dims;
    }
}
