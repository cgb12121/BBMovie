package com.bbmovie.ai_assistant_service.core.low_level._config;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelSelector;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._handler._SimpleStreamingHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ToolExecutingHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class _HandlerFactoryConfig {

    private final _ModelSelector aiSelector;

    @Autowired
    public _HandlerFactoryConfig(_ModelSelector aiSelector) {
        this.aiSelector = aiSelector;
    }

    @Bean
    @Qualifier("_AdminHandlerFactory")
    public _ChatResponseHandlerFactory adminHandlerFactory(
            @Qualifier("_AdminToolRegistry") _ToolsRegistry toolRegistry,
            _MessageService messageService,
            _ToolExecutionService toolExecutionService,
            _AuditService auditService,
            @Qualifier("_StreamingChatModel") StreamingChatModel chatModel) {

        SystemMessage systemPrompt = aiSelector.getSystemPrompt(null);

        return new _ToolExecutingHandlerFactory(
                toolRegistry,
                messageService,
                toolExecutionService,
                auditService,
                chatModel,
                systemPrompt
        );
    }

    @Bean
    @Qualifier("_ModHandlerFactory")
    public _ChatResponseHandlerFactory modHandlerFactory(
            @Qualifier("_ModToolRegistry") _ToolsRegistry toolRegistry,
            _MessageService messageService,
            _ToolExecutionService toolExecutionService,
            _AuditService auditService,
            @Qualifier("_StreamingChatModel") StreamingChatModel chatModel) {

        SystemMessage systemPrompt = aiSelector.getSystemPrompt(null);

        return new _ToolExecutingHandlerFactory(
                toolRegistry,
                messageService,
                toolExecutionService,
                auditService,
                chatModel,
                systemPrompt
        );
    }

    @Bean
    @Qualifier("_UserHandlerFactory")
    public _ChatResponseHandlerFactory userHandlerFactory(
            @Qualifier("_UserToolRegistry") _ToolsRegistry toolRegistry,
            _MessageService messageService,
            _ToolExecutionService toolExecutionService,
            _AuditService auditService,
            @Qualifier("_StreamingChatModel") StreamingChatModel chatModel) {

        SystemMessage systemPrompt = aiSelector.getSystemPrompt(null);

        return new _ToolExecutingHandlerFactory(
                toolRegistry,
                messageService,
                toolExecutionService,
                auditService,
                chatModel,
                systemPrompt
        );
    }

    @Bean
    @Qualifier("_SimpleHandlerFactory")
    public _ChatResponseHandlerFactory simpleHandlerFactory(
            _MessageService messageService,
            _AuditService auditService) {
        return new _SimpleStreamingHandlerFactory(messageService, auditService);
    }
}
