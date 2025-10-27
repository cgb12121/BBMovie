package com.bbmovie.ai_assistant_service._experimental._config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnBooleanProperty(name = "ai.experimental.enabled")
public class _LlmModelConfig {

    private static final String QWEN_3_0_6_B_Q_4_K_M = "qwen3:0.6b-q4_K_M";
    private static final String LLAMA_3_2_3_B_INSTRUCT_Q_4_K_M = "llama3.2:3b-instruct-q4_K_M";

    @Bean("experimentalStreaming")
    public StreamingChatModel streamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName(LLAMA_3_2_3_B_INSTRUCT_Q_4_K_M)
                .temperature(0.7)
                .listeners(List.of(new _ChatListener()))
                .build();
    }
}
