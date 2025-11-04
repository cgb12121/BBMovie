package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

import java.util.UUID;

public class _SimpleStreamingHandlerFactory implements _ChatResponseHandlerFactory {

    private final _MessageService messageService;
    private final _AuditService auditService;

    public _SimpleStreamingHandlerFactory(_MessageService messageService, _AuditService auditService) {
        this.messageService = messageService;
        this.auditService = auditService;
    }

    @Override
    public StreamingChatResponseHandler create(
            UUID sessionId, ChatMemory memory, FluxSink<String> sink, MonoSink<Void> monoSink) {
        return new _SimpleStreamingResponseHandler(
                sessionId,
                memory,
                sink,
                monoSink,
                messageService,
                auditService
        );
    }
}
