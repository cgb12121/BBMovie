package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._tool._ToolRegistry;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

import java.util.UUID;

public class _ToolExecutingHandlerFactory implements _ChatResponseHandlerFactory {

    private final _ToolRegistry toolRegistry;
    private final _MessageService messageService;
    private final _ToolExecutionService toolExecutionService;
    private final _AuditService auditService;
    private final StreamingChatModel chatModel;
    private final SystemMessage systemPrompt;

    public _ToolExecutingHandlerFactory(
            _ToolRegistry toolRegistry, _MessageService messageService,
            _ToolExecutionService toolExecutionService, _AuditService auditService,
            StreamingChatModel chatModel, SystemMessage systemPrompt) {
        this.toolRegistry = toolRegistry;
        this.messageService = messageService;
        this.toolExecutionService = toolExecutionService;
        this.auditService = auditService;
        this.chatModel = chatModel;
        this.systemPrompt = systemPrompt;
    }

    @Override
    public StreamingChatResponseHandler create(
            UUID sessionId, ChatMemory memory, FluxSink<String> sink, MonoSink<Void> monoSink) {
        return new _ToolExecutingResponseHandler(
                sessionId, memory, sink, monoSink, chatModel, systemPrompt,
                toolRegistry, messageService, toolExecutionService, auditService,
                System.currentTimeMillis()
        );
    }
}