package com.example.bbmoviesearch.service.embedding;

import ai.djl.Device;
import ai.djl.ModelException;
import ai.djl.engine.Engine;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;
import com.example.bbmoviesearch.exception.EmbeddingException;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class LocalEmbeddingService implements AutoCloseable, EmbeddingService {

    private final ZooModel<String, float[]> model;
    private final ThreadLocal<Predictor<String, float[]>> predictorThreadLocal;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    public LocalEmbeddingService(RedisTemplate<Object, Object> redisTemplate) throws IOException, ModelException {
        Device device = Engine.getInstance().getGpuCount() > 0 ? Device.gpu() : Device.cpu();
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2")
                .optEngine("PyTorch")
                .optDevice(device)
                .optProgress(new ProgressBar())
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .build();
        this.model = ModelZoo.loadModel(criteria);
        this.predictorThreadLocal = ThreadLocal.withInitial(model::newPredictor);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public float[] generateEmbedding(String text) {
        String key = "embedding:" + StringUtils.deleteWhitespace(text);
        float[] cachedEmbedding = (float[]) redisTemplate.opsForValue().get(key);
        if (cachedEmbedding != null) {
            return cachedEmbedding;
        }
        Predictor<String, float[]> predictor = predictorThreadLocal.get();
        try {
            float[] embedding = predictor.predict(text);
            redisTemplate.opsForValue().set(key, embedding, 5, TimeUnit.MINUTES);
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
                String key = "embedding:" + StringUtils.deleteWhitespace(texts.get(i));
                redisTemplate.opsForValue().set(key, embeddings[i], 5, TimeUnit.MINUTES);
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