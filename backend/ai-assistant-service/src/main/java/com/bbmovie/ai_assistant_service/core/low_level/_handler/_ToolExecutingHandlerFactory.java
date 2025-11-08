package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelFactory;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

import java.util.UUID;

public class _ToolExecutingHandlerFactory implements _ChatResponseHandlerFactory {

    private final _ToolsRegistry toolRegistry;
    private final _MessageService messageService;
    private final _ToolExecutionService toolExecutionService;
    private final _AuditService auditService;
    private final _ModelFactory modelFactory;
    private final SystemMessage systemPrompt;

    public _ToolExecutingHandlerFactory(
            _ToolsRegistry toolRegistry,
            _MessageService messageService,
            _ToolExecutionService toolExecutionService,
            _AuditService auditService,
            _ModelFactory modelFactory,
            SystemMessage systemPrompt) {
        this.toolRegistry = toolRegistry;
        this.messageService = messageService;
        this.toolExecutionService = toolExecutionService;
        this.auditService = auditService;
        this.modelFactory = modelFactory;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public StreamingChatResponseHandler create(
            UUID sessionId, ChatMemory memory, FluxSink<String> sink, MonoSink<Void> monoSink) {
        return new _ToolExecutingResponseHandler(
                sessionId,
                memory,
                sink,
                monoSink,
                modelFactory,
                systemPrompt,
                toolRegistry,
                messageService,
                toolExecutionService,
                auditService,
                System.currentTimeMillis()
        );
    }
}