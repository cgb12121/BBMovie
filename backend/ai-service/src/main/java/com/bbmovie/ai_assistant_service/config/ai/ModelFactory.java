package com.bbmovie.ai_assistant_service.config.ai;

import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ModelFactory {

    @Qualifier("nonThinkingModel") private final StreamingChatModel normalModel;
    @Qualifier("thinkingModel") private final StreamingChatModel thinkingModel;


    public StreamingChatModel getModel(AiMode mode) {
        return switch (mode) {
            case THINKING -> thinkingModel;
            case NORMAL -> normalModel;
            case FAST -> normalModel; //TODO: implement a fast model
            case CREATIVE -> normalModel; //TODO: implement a creative model
            case REASONING -> thinkingModel; // TODO: implement a reasoning model
            case null -> throw new IllegalArgumentException("Invalid AI mode.");
        };
    }
}