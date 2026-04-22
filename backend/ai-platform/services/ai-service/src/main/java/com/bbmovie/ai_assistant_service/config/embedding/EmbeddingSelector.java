package com.bbmovie.ai_assistant_service.config.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmbeddingSelector {

    private final EmbeddingProperties embeddingProperties;
    private final EmbeddingIndexProperties indexProperties;

    public String getModelName() {
        return embeddingProperties.getModelName();
    }

    public String getIndex() {
        return embeddingProperties.getIndex();
    }

    public int getDimension() {
        return embeddingProperties.getDimension();
    }

    public String getEmbeddingField() {
        return embeddingProperties.getEmbeddingField();
    }

    public String getMovieIndex() { return indexProperties.getMovies(); }

    public String getRagIndex() { return indexProperties.getRag(); }
}
