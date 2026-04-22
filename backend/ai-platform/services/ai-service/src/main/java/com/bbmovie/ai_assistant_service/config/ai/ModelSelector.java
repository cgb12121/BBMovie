package com.bbmovie.ai_assistant_service.config.ai;

import com.bbmovie.ai_assistant_service.utils.AiModel;
import com.bbmovie.ai_assistant_service.utils.PromptLoader;
import dev.langchain4j.data.message.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.lang.Nullable;
import java.util.Map;

@Component
public class ModelSelector {

    private final ModelProperties aiProperties;

    @Autowired
    public ModelSelector(ModelProperties aiProperties) {
        this.aiProperties = aiProperties;
    }

    /**
     * Returns the currently active AI model.
     */
    public AiModel getActiveModel() {
        return AiModel.valueOf(aiProperties.getActiveModel().toUpperCase());
    }

    /**
     * Builds the system prompt based on the current configuration.
     */
    public SystemMessage getSystemPrompt(@Nullable Map<String, Object> vars) {
        return PromptLoader.loadSystemPrompt(
                aiProperties.isEnablePersona(),
                getActiveModel(),
                vars
        );
    }

    /**
     * Convenience: getWithCursor the model name string for Ollama API.
     */
    public String getModelName() {
        return getActiveModel().getModelName();
    }
}
