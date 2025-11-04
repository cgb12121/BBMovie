package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelSelector;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantMetadata;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._service._impl._AuditServiceImpl;
import com.bbmovie.ai_assistant_service.core.low_level._service._impl._MessageServiceImpl;
import com.bbmovie.ai_assistant_service.core.low_level._service._RagService;
import com.bbmovie.ai_assistant_service.core.low_level._config._ToolsRegistry;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class _ModAssistant extends _BaseAssistant {

    private final _ChatResponseHandlerFactory handlerFactory;

    @Autowired
    public _ModAssistant(
            @Qualifier("_StreamingChatModel") StreamingChatModel chatModel,
            @Qualifier("_ChatMemoryProvider") ChatMemoryProvider chatMemoryProvider,
            @Qualifier("_ModToolRegistry") _ToolsRegistry toolRegistry,
            @Qualifier("_ModHandlerFactory") _ChatResponseHandlerFactory handlerFactory,
            _MessageServiceImpl chatMessageService, _AuditServiceImpl auditService,
            _ModelSelector aiSelector, _RagService ragService) {
        super(
                chatModel,
                chatMemoryProvider,
                chatMessageService,
                auditService,
                toolRegistry,
                aiSelector.getSystemPrompt(null),
                buildMetadata(chatModel, toolRegistry),
                ragService
        );
        this.handlerFactory = handlerFactory;
    }

    @Override
    public _AssistantType getType() {
        return _AssistantType.MOD;
    }

    @Override
    protected _ChatResponseHandlerFactory getHandlerFactory() {
        return this.handlerFactory;
    }

    private static AssistantMetadata buildMetadata(StreamingChatModel model, _ToolsRegistry registry) {
        return AssistantMetadata.builder()
                .type(_AssistantType.MOD)
                .modelName(model.toString())
                .description("Moderator assistant for content management and user support.")
                .capabilities(registry.getToolSpecifications()
                        .stream()
                        .map(spec -> spec.name() + ": " + spec.description())
                        .toList()
                )
                .build();
    }
}
