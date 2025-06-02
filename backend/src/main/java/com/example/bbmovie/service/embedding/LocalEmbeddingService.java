package com.example.bbmovie.service.embedding;

import ai.djl.Device;
import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import com.example.bbmovie.exception.EmbeddingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Log4j2
@Service
public class LocalEmbeddingService implements AutoCloseable, EmbeddingService {

    private final ZooModel<String, float[]> model;
    private final ThreadLocal<Predictor<String, float[]>> predictorThreadLocal;
    private final Cache<String, float[]> embeddingCache = CacheBuilder.newBuilder()
            .maximumSize(10000L)
            .expireAfterWrite(Duration.ofMinutes(5))
            .build();

    public LocalEmbeddingService() throws IOException, ModelException {
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
                .optEngine("PyTorch")
                .optDevice(Device.gpu())
                .optProgress(new ProgressBar())
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .build();

        this.model = criteria.loadModel();
        this.predictorThreadLocal = ThreadLocal.withInitial(model::newPredictor);
    }

    @Override
    public float[] generateEmbedding(String text) {
        float[] cachedEmbedding = embeddingCache.getIfPresent(text);
        if (cachedEmbedding != null) {
            return cachedEmbedding;
        }
        Predictor<String, float[]> predictor = predictorThreadLocal.get();
        try {
            float[] embedding = predictor.predict(text);
            embeddingCache.put(text, embedding);
            return embedding;
        } catch (TranslateException e) {
            log.error("Failed to generate embedding for text: {}", text, e);
            throw new EmbeddingException("Unable to generate embedding for text.");
        }
    }

    @Override
    public float[][] generateEmbeddings(List<String> texts) {
        Predictor<String, float[]> predictor = predictorThreadLocal.get();
        try {
            float[][] embeddings = predictor.batchPredict(texts).toArray(new float[0][]);
            for (int i = 0; i < texts.size(); i++) {
                embeddingCache.put(texts.get(i), embeddings[i]);
            }
            return embeddings;
        } catch (TranslateException e) {
            log.error("Failed to generate embeddings for batch", e);
            throw new EmbeddingException("Unable to generate embeddings for batch.");
        }
    }

    @Override
    @PreDestroy
    public void close() {
        predictorThreadLocal.get().close();
        predictorThreadLocal.remove();
        if (model != null) {
            model.close();
        }
    }
}