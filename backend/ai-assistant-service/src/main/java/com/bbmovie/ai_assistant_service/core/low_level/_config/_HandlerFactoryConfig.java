package com.bbmovie.ai_assistant_service.core.low_level._config;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelSelector;
import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ToolResponseHandler;
import com.bbmovie.ai_assistant_service.core.low_level._handler._processor._SimpleResponseProcessor;
import com.bbmovie.ai_assistant_service.core.low_level._handler._processor._ToolResponseProcessor;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolWorkflowFacade;
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
            _ToolWorkflowFacade toolWorkflowFacade,
            _ModelSelector modelSelector,
            @Qualifier("_AdminToolRegistry") _ToolsRegistry toolRegistry
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
    @Qualifier("_ModHandlerFactory")
    public _ChatResponseHandlerFactory modHandlerFactory(
            _AuditService auditService,
            _MessageService messageService,
            _ToolWorkflowFacade toolWorkflowFacade,
            _ModelSelector modelSelector,
            @Qualifier("_ModToolRegistry") _ToolsRegistry toolRegistry
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
    @Qualifier("_UserHandlerFactory")
    public _ChatResponseHandlerFactory userHandlerFactory(
            _AuditService auditService,
            _MessageService messageService,
            _ToolWorkflowFacade toolWorkflowFacade,
            _ModelSelector modelSelector,
            @Qualifier("_UserToolRegistry") _ToolsRegistry toolRegistry
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
    @Qualifier("_SimpleHandlerFactory")
    public _ChatResponseHandlerFactory simpleHandlerFactory(
            _AuditService auditService,
            _MessageService messageService,
            _ToolWorkflowFacade toolWorkflowFacade,
            _ModelSelector modelSelector
    ) {
        return createHandlerFactory(
                auditService,
                messageService,
                toolWorkflowFacade,
                modelSelector,
                null,
                false
        );
    }

    private _ChatResponseHandlerFactory createHandlerFactory(
            _AuditService auditService,
            _MessageService messageService,
            _ToolWorkflowFacade toolWorkflowFacade,
            _ModelSelector modelSelector,
            _ToolsRegistry toolRegistry,
            boolean enablePersona) {

        SystemMessage systemPrompt = _PromptLoader.loadSystemPrompt(
                enablePersona, modelSelector.getActiveModel(), null);

        return (sessionId, chatMemory, sink, monoSink, aiMode) -> {
            long requestStartTime = System.currentTimeMillis();

            _SimpleResponseProcessor simpleProcessor = new _SimpleResponseProcessor.Builder()
                    .sessionId(sessionId)
                    .chatMemory(chatMemory)
                    .auditService(auditService)
                    .messageService(messageService)
                    .build();

            _ToolResponseProcessor toolProcessor = new _ToolResponseProcessor.Builder()
                    .sessionId(sessionId)
                    .aiMode(aiMode)
                    .chatMemory(chatMemory)
                    .toolRegistry(toolRegistry)
                    .systemPrompt(systemPrompt)
                    .toolWorkflowFacade(toolWorkflowFacade)
                    .sink(sink)
                    .requestStartTime(requestStartTime)
                    .build();

            return _ToolResponseHandler.builder()
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
