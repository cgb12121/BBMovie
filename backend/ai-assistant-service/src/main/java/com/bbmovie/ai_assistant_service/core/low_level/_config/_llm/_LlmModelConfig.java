package com.bbmovie.ai_assistant_service.core.low_level._config._llm;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@SuppressWarnings("unused")
@Configuration
public class _LlmModelConfig {

    private static final String QWEN3_0_6_B = "qwen3:0.6b";
    private static final String HERMES3_3_B = "hermes3:3b";
    private static final String NEMOTRON_MINI_4_B = "nemotron-mini:4b";
    private static final String QWEN3_1_7_B = "qwen3:1.7b";
    private static final String LLAMA3$2_3_B = "llama3.2:3b";

    @Bean("_StreamingChatModel")
    public StreamingChatModel _StreamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName(LLAMA3$2_3_B)
                .temperature(0.7)
                .listeners(List.of(new _ChatListener()))
                .build();
    }
}
