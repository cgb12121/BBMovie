package com.bbmovie.ai_assistant_service.core.low_level._config._ai;

import com.bbmovie.ai_assistant_service.core.low_level._utils._AiModel;
import com.bbmovie.ai_assistant_service.core.low_level._utils._PromptLoader;
import dev.langchain4j.data.message.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.lang.Nullable;
import java.util.Map;

@Component
public class _ModelSelector {

    private final _ModelProperties aiProperties;

    @Autowired
    public _ModelSelector(_ModelProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    /**
     * Returns the currently active AI model.
     */
    public _AiModel getActiveModel() {
        return _AiModel.valueOf(aiProperties.getActiveModel().toUpperCase());
    }

    /**
     * Builds the system prompt based on the current configuration.
     */
    public SystemMessage getSystemPrompt(@Nullable Map<String, Object> vars) {
        return _PromptLoader.loadSystemPrompt(
                aiProperties.isEnablePersona(),
                getActiveModel(),
                vars
        );
    }

    /**
     * Convenience: get the model name string for Ollama API.
     */
    public String getModelName() {
        return getActiveModel().getModelName();
    }
}
