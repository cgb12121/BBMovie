package com.example.bbmovieragrecommendation.service.llm;

import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDArray;

public class SimpleTextGenerationTranslator implements Translator<String, String> {

    private HuggingFaceTokenizer tokenizer;

    @Override
    public void prepare(TranslatorContext ctx) {
        tokenizer = HuggingFaceTokenizer.newInstance("gpt2");
    }

    @Override
    @SuppressWarnings("all")
    public NDList processInput(TranslatorContext ctx, String input) throws Exception {
        long[] inputIds = tokenizer.encode(input).getIds();
        NDArray array = ctx.getNDManager().create(inputIds);
        array = array.expandDims(0);
        return new NDList(array);
    }

    @Override
    public String processOutput(TranslatorContext ctx, NDList list) {
        NDArray result = list.getFirst();
        long[] predictedTokenIds = result.toLongArray();
        return tokenizer.decode(predictedTokenIds);
    }

    @Override
    public Batchifier getBatchifier() {
        return null; // disable batching for now
    }
}
