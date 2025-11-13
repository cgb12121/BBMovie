package com.bbmovie.ai_assistant_service.core.low_level._config._ai;

import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class _ModelFactory {

    private final StreamingChatModel normalModel;
    private final StreamingChatModel thinkingModel;

    @Autowired
    public _ModelFactory(
            @Qualifier("_NonThinkingModel") StreamingChatModel normalModel,
            @Qualifier("_ThinkingModel") StreamingChatModel thinkingModel) {
        this.normalModel = normalModel;
        this.thinkingModel = thinkingModel;
    }

    public StreamingChatModel getModel(_AiMode mode) {
        return switch (mode) {
            case THINKING -> thinkingModel;
            case NORMAL -> normalModel;
            case FAST -> normalModel; //TODO: implement a fast model
            case CREATIVE -> normalModel; //TODO: implement a creative model
            case REASONING -> thinkingModel; // TODO: implement a reasoning model
        };
    }
}