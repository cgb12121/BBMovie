package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantMetadata;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatMessageService;
import com.bbmovie.ai_assistant_service.core.low_level._tool._ToolRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._utils._AiModel;
import com.bbmovie.ai_assistant_service.core.low_level._utils._PromptLoader;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class _AdminAssistant extends _BaseAssistant {

    private final _ChatResponseHandlerFactory handlerFactory;

    public _AdminAssistant(
            @Qualifier("_StreamingChatModel") StreamingChatModel chatModel,
            @Qualifier("_ChatMemoryProvider") ChatMemoryProvider chatMemoryProvider,
            @Qualifier("_AdminToolRegistry") _ToolRegistry toolRegistry,
            @Qualifier("_AdminHandlerFactory") _ChatResponseHandlerFactory handlerFactory,
            _ChatMessageService chatMessageService,
            _AuditService auditService) {
        super(chatModel, chatMemoryProvider, chatMessageService, auditService, toolRegistry,
                _PromptLoader.loadSystemPrompt(true, _AiModel.LLAMA3, null),
                buildMetadata(chatModel, toolRegistry));
        this.handlerFactory = handlerFactory;
    }

    @Override
    public AssistantType getType() {
        return AssistantType.ADMIN;
    }

    @Override
    protected _ChatResponseHandlerFactory getHandlerFactory() {
        return this.handlerFactory;
    }

    private static AssistantMetadata buildMetadata(StreamingChatModel model, _ToolRegistry registry) {
        return AssistantMetadata.builder()
                .type(AssistantType.ADMIN)
                .modelName(model.toString())
                .description("Administrative assistant with tool execution capabilities.")
                .capabilities(registry.getToolSpecifications().stream().map(spec -> spec.name() + ": " + spec.description()).toList())
                .build();
    }
}