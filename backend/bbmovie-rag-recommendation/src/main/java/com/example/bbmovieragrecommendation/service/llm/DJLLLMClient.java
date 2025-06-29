package com.example.bbmovieragrecommendation.service.llm;

import ai.djl.ModelException;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.TranslateException;

import java.io.IOException;

public class DJLLLMClient {

    private final ZooModel<String, String> model;
    private final Predictor<String, String> predictor;

    public DJLLLMClient() throws IOException, ModelException {
        Criteria<String, String> criteria = Criteria.builder()
                .setTypes(String.class, String.class)
                .optModelUrls("djl://ai.djl.huggingface.pytorch/gpt2")
                .optTranslator(new SimpleTextGenerationTranslator())
                .optEngine("PyTorch")
                .optProgress(new ProgressBar())
                .build();

        model = ModelZoo.loadModel(criteria);
        predictor = model.newPredictor();
    }

    public String predict(String input) throws TranslateException {
        return predictor.predict(input);
    }

    public void close() {
        predictor.close();
        model.close();
    }
}