package com.example.bbmovie.service.embedding;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class LocalEmbeddingService implements AutoCloseable {

    private final ZooModel<String, float[]> model;
    private final Predictor<String, float[]> predictor;

    public LocalEmbeddingService() throws IOException, ModelException {
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
                .optEngine("PyTorch")
                .optProgress(new ProgressBar())
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .build();

        this.model = criteria.loadModel();
        this.predictor = model.newPredictor();
    }

    public float[] generateEmbedding(String text) throws TranslateException {
        return predictor.predict(text);
    }

    @PreDestroy
    @Override
    public void close() {
        if (predictor != null) {
            predictor.close();
        }
        if (model != null) {
            model.close();
        }
    }
}