package com.bbmovie.ai_assistant_service.config.ai;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;

import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ModelFactory {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(ModelFactory.class);

    @Qualifier("nonThinkingModel") private final StreamingChatModel normalModel;
    @Qualifier("thinkingModel") private final StreamingChatModel thinkingModel;


    public StreamingChatModel getModel(AiMode mode) {
        StreamingChatModel selectedModel = switch (mode) {
            case THINKING -> {
                log.debug("[ModelFactory] Selecting THINKING model for mode: {}", mode);
                yield thinkingModel;
            }
            case NORMAL -> {
                log.debug("[ModelFactory] Selecting NORMAL model for mode: {}", mode);
                yield normalModel;
            }
            case FAST -> {
                log.debug("[ModelFactory] Selecting NORMAL model (FAST not implemented) for mode: {}", mode);
                yield normalModel; //TODO: implement a fast model
            }
            case CREATIVE -> {
                log.debug("[ModelFactory] Selecting NORMAL model (CREATIVE not implemented) for mode: {}", mode);
                yield normalModel; //TODO: implement a creative model
            }
            case REASONING -> {
                log.debug("[ModelFactory] Selecting THINKING model (REASONING not implemented) for mode: {}", mode);
                yield thinkingModel; // TODO: implement a reasoning model
            }
            case null -> throw new IllegalArgumentException("Invalid AI mode: null");
        };
        log.debug("[ModelFactory] Selected model: {}", selectedModel == thinkingModel ? "THINKING" : "NORMAL");
        return selectedModel;
    }
}