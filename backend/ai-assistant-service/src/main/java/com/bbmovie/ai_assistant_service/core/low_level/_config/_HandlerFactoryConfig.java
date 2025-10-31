package com.bbmovie.ai_assistant_service.core.low_level._config;

import com.bbmovie.ai_assistant_service.core.low_level._handler._ChatResponseHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ToolExecutingHandlerFactory;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatMessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._tool._ToolRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._utils._AiModel;
import com.bbmovie.ai_assistant_service.core.low_level._utils._PromptLoader;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class _HandlerFactoryConfig {

    @Bean
    @Qualifier("_AdminHandlerFactory")
    public _ChatResponseHandlerFactory adminHandlerFactory(
            @Qualifier("_AdminToolRegistry") _ToolRegistry toolRegistry,
            _ChatMessageService messageService,
            _ToolExecutionService toolExecutionService,
            _AuditService auditService,
            @Qualifier("_StreamingChatModel") StreamingChatModel chatModel) {

        SystemMessage systemPrompt = _PromptLoader.loadSystemPrompt(true, _AiModel.LLAMA3, null);

        return new _ToolExecutingHandlerFactory(
                toolRegistry,
                messageService,
                toolExecutionService,
                auditService,
                chatModel,
                systemPrompt
        );
    }
}
