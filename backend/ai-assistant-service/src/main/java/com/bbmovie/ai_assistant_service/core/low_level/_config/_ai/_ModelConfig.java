package com.bbmovie.ai_assistant_service.core.low_level._config._ai;

import com.bbmovie.ai_assistant_service.core.low_level._utils._AiModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class _ModelConfig {

    @Bean("_StreamingChatModel")
    public StreamingChatModel _StreamingChatModel() {
        return OllamaStreamingChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName(_AiModel.QWEN3.getModelName())
                .temperature(0.7)
                .listeners(List.of(new _ChatListener()))
                .build();
    }
}
