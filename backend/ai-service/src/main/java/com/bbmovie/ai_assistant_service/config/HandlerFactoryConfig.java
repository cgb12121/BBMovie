package com.bbmovie.ai_assistant_service.config;

import com.bbmovie.ai_assistant_service.config.ai.ModelSelector;
import com.bbmovie.ai_assistant_service.config.tool.ToolsRegistry;
import com.bbmovie.ai_assistant_service.handler.ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.handler.ToolResponseHandler;
import com.bbmovie.ai_assistant_service.handler.processor.SimpleResponseProcessor;
import com.bbmovie.ai_assistant_service.handler.processor.ToolResponseProcessor;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.service.MessageService;
import com.bbmovie.ai_assistant_service.service.facade.ToolWorkflow;
import com.bbmovie.ai_assistant_service.utils.PromptLoader;
import dev.langchain4j.data.message.SystemMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HandlerFactoryConfig {

    @Bean
    @Qualifier("adminHandlerFactory")
    public ChatResponseHandlerFactory adminHandlerFactory(
            AuditService auditService, MessageService messageService,
            ToolWorkflow toolWorkflowFacade, ModelSelector modelSelector,
            @Qualifier("adminToolRegistry") ToolsRegistry toolRegistry
    ) {
        return createHandlerFactory(
                auditService,
                messageService,
                toolWorkflowFacade,
                modelSelector,
                toolRegistry,
                true
        );
    }

    @Bean
    @Qualifier("modHandlerFactory")
    public ChatResponseHandlerFactory modHandlerFactory(
            AuditService auditService, MessageService messageService,
            ToolWorkflow toolWorkflowFacade, ModelSelector modelSelector,
            @Qualifier("modToolRegistry") ToolsRegistry toolRegistry
    ) {
        return createHandlerFactory(
                auditService,
                messageService,
                toolWorkflowFacade,
                modelSelector,
                toolRegistry,
                true
        );
    }

    @Bean
    @Qualifier("userHandlerFactory")
    public ChatResponseHandlerFactory userHandlerFactory(
            AuditService auditService, MessageService messageService,
            ToolWorkflow toolWorkflowFacade, ModelSelector modelSelector,
            @Qualifier("userToolRegistry") ToolsRegistry toolRegistry
    ) {
        return createHandlerFactory(
                auditService,
                messageService,
                toolWorkflowFacade,
                modelSelector,
                toolRegistry,
                true
        );
    }

    @Bean
    @Qualifier("simpleHandlerFactory")
    public ChatResponseHandlerFactory simpleHandlerFactory(
            AuditService auditService, MessageService messageService,
            ToolWorkflow toolWorkflowFacade, ModelSelector modelSelector) {
        return createHandlerFactory(
                auditService,
                messageService,
                toolWorkflowFacade,
                modelSelector,
                null,
                false
        );
    }

    private ChatResponseHandlerFactory createHandlerFactory(
            AuditService auditService, MessageService messageService, ToolWorkflow toolWorkflowFacade,
            ModelSelector modelSelector, ToolsRegistry toolRegistry, boolean enablePersona) {

        SystemMessage systemPrompt = PromptLoader.loadSystemPrompt(
                enablePersona, modelSelector.getActiveModel(), null);

        return (sessionId, chatMemory, sink, monoSink, aiMode) -> {
            long requestStartTime = System.currentTimeMillis();

            SimpleResponseProcessor simpleProcessor = SimpleResponseProcessor.builder()
                    .sessionId(sessionId)
                    .chatMemory(chatMemory)
                    .auditService(auditService)
                    .messageService(messageService)
                    .build();

            ToolResponseProcessor toolProcessor = ToolResponseProcessor.builder()
                    .sessionId(sessionId)
                    .aiMode(aiMode)
                    .chatMemory(chatMemory)
                    .toolRegistry(toolRegistry)
                    .systemPrompt(systemPrompt)
                    .toolWorkflowFacade(toolWorkflowFacade)
                    .sink(sink)
                    .requestStartTime(requestStartTime)
                    .build();

            return ToolResponseHandler.builder()
                    .sink(sink)
                    .monoSink(monoSink)
                    .simpleProcessor(simpleProcessor)
                    .toolProcessor(toolProcessor)
                    .requestStartTime(requestStartTime)
                    .auditService(auditService)
                    .sessionId(sessionId)
                    .build();
        };
    }
}
