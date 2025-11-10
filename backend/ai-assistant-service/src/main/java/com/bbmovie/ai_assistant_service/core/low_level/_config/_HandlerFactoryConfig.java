package com.bbmovie.ai_assistant_service.core.low_level._config;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelFactory;
import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelSelector;
import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._handler._DefaultResponseHandler;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._utils._PromptLoader;
import dev.langchain4j.data.message.SystemMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class _HandlerFactoryConfig {

    @Bean
    @Qualifier("_AdminHandlerFactory")
    public _ChatResponseHandlerFactory adminHandlerFactory(
            _AuditService auditService,
            _MessageService messageService,
            _ToolExecutionService toolExecutionService,
            _ModelFactory modelFactory,
            _ModelSelector modelSelector,
            @Qualifier("_AdminToolRegistry") _ToolsRegistry toolRegistry
    ) {
        return createHandlerFactory(
                auditService,
                messageService,
                toolExecutionService,
                modelFactory,
                modelSelector,
                toolRegistry,
                true
        );
    }

    @Bean
    @Qualifier("_ModHandlerFactory")
    public _ChatResponseHandlerFactory modHandlerFactory(
            _AuditService auditService,
            _MessageService messageService,
            _ToolExecutionService toolExecutionService,
            _ModelFactory modelFactory,
            _ModelSelector modelSelector,
            @Qualifier("_ModToolRegistry") _ToolsRegistry toolRegistry
    ) {
        return createHandlerFactory(
                auditService,
                messageService,
                toolExecutionService,
                modelFactory,
                modelSelector,
                toolRegistry,
                true
        );
    }

    @Bean
    @Qualifier("_UserHandlerFactory")
    public _ChatResponseHandlerFactory userHandlerFactory(
            _AuditService auditService,
            _MessageService messageService,
            _ToolExecutionService toolExecutionService,
            _ModelFactory modelFactory,
            _ModelSelector modelSelector,
            @Qualifier("_UserToolRegistry") _ToolsRegistry toolRegistry
    ) {
        return createHandlerFactory(
                auditService,
                messageService,
                toolExecutionService,
                modelFactory,
                modelSelector,
                toolRegistry,
                true
        );
    }

    // This factory is for assistants that DO NOT use tools.
    @Bean
    @Qualifier("_SimpleHandlerFactory")
    public _ChatResponseHandlerFactory simpleHandlerFactory(
            _AuditService auditService,
            _MessageService messageService,
            _ModelFactory modelFactory,
            _ModelSelector modelSelector
    ) {
        return createHandlerFactory(
                auditService,
                messageService,
                null,
                modelFactory,
                modelSelector,
                null,
                false
        );
    }

    private _ChatResponseHandlerFactory createHandlerFactory(
            _AuditService auditService,
            _MessageService messageService,
            _ToolExecutionService toolExecutionService,
            _ModelFactory modelFactory,
            _ModelSelector modelSelector,
            _ToolsRegistry toolRegistry,
            boolean enablePersona) {

        SystemMessage systemPrompt = _PromptLoader.loadSystemPrompt(
                enablePersona, modelSelector.getActiveModel(), null);

        return (sessionId, chatMemory, sink, monoSink, aiMode) ->
                new _DefaultResponseHandler.Builder()
                        .sessionId(sessionId)
                        .aiMode(aiMode)
                        .chatMemory(chatMemory)
                        .modelFactory(modelFactory)
                        .systemPrompt(systemPrompt)
                        .toolRegistry(toolRegistry)
                        .messageService(messageService)
                        .toolExecutionService(toolExecutionService)
                        .auditService(auditService)
                        .requestStartTime(System.currentTimeMillis())
                        .sink(sink)
                        .monoSink(monoSink)
                        .build();
    }
}
