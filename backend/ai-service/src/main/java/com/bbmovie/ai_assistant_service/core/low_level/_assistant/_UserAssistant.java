package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelFactory;
import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelSelector;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AssistantMetadata;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._service._impl._AuditServiceImpl;
import com.bbmovie.ai_assistant_service.core.low_level._service._impl._MessageServiceImpl;
import com.bbmovie.ai_assistant_service.core.low_level._service._RagService;
import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class _UserAssistant extends _BaseAssistant {

    private final _ChatResponseHandlerFactory handlerFactory;

    @Autowired
    public  _UserAssistant(
            _ModelFactory modelFactory,
            @Qualifier("_ChatMemoryProvider") ChatMemoryProvider chatMemoryProvider,
            @Qualifier("_UserToolRegistry") _ToolsRegistry toolRegistry,
            @Qualifier("_UserHandlerFactory") _ChatResponseHandlerFactory handlerFactory,
            _MessageServiceImpl chatMessageService, _AuditServiceImpl auditService,
            _ModelSelector aiSelector, _RagService ragService) {
        super(
                modelFactory,
                chatMemoryProvider,
                chatMessageService,
                auditService,
                toolRegistry,
                aiSelector.getSystemPrompt(null),
                buildMetadata(modelFactory.getModel(_AiMode.THINKING), toolRegistry),
                ragService
        );
        this.handlerFactory = handlerFactory;
    }

    @Override
    protected _ChatResponseHandlerFactory getHandlerFactory() {
        return this.handlerFactory;
    }

    @Override
    public _AssistantType getType() {
        return _AssistantType.USER;
    }

    private static _AssistantMetadata buildMetadata(StreamingChatModel model, _ToolsRegistry registry) {
        return _AssistantMetadata.builder()
                .type(_AssistantType.USER)
                .modelName(model.toString())
                .description("Administrative assistant with tool execution capabilities.")
                .capabilities(registry.getToolSpecifications()
                        .stream()
                        .map(spec -> spec.name() + ": " + spec.description())
                        .toList()
                )
                .build();
    }
}
