package com.bbmovie.ai_assistant_service.core.low_level._config._embedding;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class _EmbeddingSelector {

    private final _EmbeddingProperties embeddingProperties;
    private final _EmbeddingIndexProperties indexProperties;

    @Autowired
    public _EmbeddingSelector(_EmbeddingProperties embeddingProperties, _EmbeddingIndexProperties indexProperties) {
        this.embeddingProperties = embeddingProperties;
        this.indexProperties = indexProperties;
    }

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
