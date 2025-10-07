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
import ai.djl.util.cuda.CudaUtils;
import com.example.bbmoviesearch.exception.EmbeddingException;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
import java.math.RoundingMode;


@Log4j2
@Service
@ConditionalOnProperty(
        prefix = "embedding.provider.djl",
        name = "enabled",
        havingValue = "true"
)
public class DjLEmbeddingService implements AutoCloseable, EmbeddingService {

    private final ZooModel<String, float[]> model;
    private final ThreadLocal<Predictor<String, float[]>> predictorThreadLocal;

    public DjLEmbeddingService() throws IOException, ModelException {
        Device device = setupEngine();
        Criteria<String, float[]> criteria = setupModel(device);
        this.model = ModelZoo.loadModel(criteria);
        this.predictorThreadLocal = ThreadLocal.withInitial(model::newPredictor);
    }

    private static Device setupEngine() {
        log.info("Setup DjL Engine...");
        log.info("DjL version: {}", Engine.getDjlVersion());

        int gpuCount = Engine.getInstance().getGpuCount();
        log.info("GPU count: {}", gpuCount);

        Device device;
        if (gpuCount > 0) {
            log.info("Detected GPU(s): {}", gpuCount);
            log.info("Setting DjL Engine to GPU...");
            device = Device.gpu();

            int cudaVersion = CudaUtils.getCudaVersion();
            MemoryUsage vRam = CudaUtils.getGpuMemory(device);

            log.info("CUDA version: {}", cudaVersion);
            log.info("GPU v-ram: {} GB", BigDecimal.valueOf(vRam.getMax())
                    .divide(BigDecimal.valueOf(1073741824), 2, RoundingMode.FLOOR));
        } else {
            log.info("No GPU detected. Setting DjL Engine to CPU...");
            device = Device.cpu();
        }

        log.info("DjL Engine setup complete. {}", device.toString());
        return device;
    }

    private static Criteria<String, float[]> setupModel(Device device) {
        Criteria<String, float[]> criteria = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optModelUrls(DjLModelOptions.MONOLINGUAL_ENGLISH_ONLY_MODEL.getModelUri())
                .optEngine("PyTorch")
                .optDevice(device)
                .optProgress(new ProgressBar())
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .build();
        log.info("Setup DjL Model: {}", criteria.toString());
        return criteria;
    }

    @Override
    public Mono<float[]> generateEmbedding(String text) {
        return Mono.fromCallable(() -> {
                    Predictor<String, float[]> predictor = predictorThreadLocal.get();
                    return predictor.predict(text);
                }).subscribeOn(Schedulers.boundedElastic())
                .onErrorMap(e -> {
                    log.error("Failed to generate embedding for text: {}", text, e);
                    return new EmbeddingException("Unable to generate embedding for text.");
                });
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