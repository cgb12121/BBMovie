package com.bbmovie.ai_assistant_service.assistant;

import com.bbmovie.ai_assistant_service.config.ai.ModelFactory;
import com.bbmovie.ai_assistant_service.config.ai.ModelSelector;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.entity.model.AssistantMetadata;
import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import com.bbmovie.ai_assistant_service.handler.ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.service.MessageService;
import com.bbmovie.ai_assistant_service.service.RagService;
import com.bbmovie.ai_assistant_service.config.tool.ToolsRegistry;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AdminAssistant extends BaseAssistant {

    private final ChatResponseHandlerFactory handlerFactory;

    @Autowired
    public AdminAssistant(
            ModelFactory modelFactory,
            @Qualifier("ChatMemoryProvider") ChatMemoryProvider chatMemoryProvider,
            @Qualifier("adminToolRegistry") ToolsRegistry toolRegistry,
            @Qualifier("adminHandlerFactory") ChatResponseHandlerFactory handlerFactory,
            MessageService chatMessageService, AuditService auditService,
            ModelSelector aiSelector, RagService ragService) {
        super(
            modelFactory,
            chatMemoryProvider,
            chatMessageService,
            auditService,
            toolRegistry,
            aiSelector.getSystemPrompt(null),
            buildMetadata(modelFactory.getModel(AiMode.THINKING), toolRegistry),
            ragService
        );
        this.handlerFactory = handlerFactory;
    }

    @Override
    public AssistantType getType() {
        return AssistantType.ADMIN;
    }

    @Override
    protected ChatResponseHandlerFactory getHandlerFactory() {
        return this.handlerFactory;
    }

    private static AssistantMetadata buildMetadata(StreamingChatModel model, ToolsRegistry registry) {
        return AssistantMetadata.builder()
                .type(AssistantType.ADMIN)
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