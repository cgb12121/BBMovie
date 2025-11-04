package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.UUID;

@Slf4j
public class _SimpleStreamingResponseHandler extends _BaseResponseHandler {

    private final UUID sessionId;
    private final ChatMemory chatMemory;
    private final _MessageService messageService;
    private final _AuditService auditService;

    public _SimpleStreamingResponseHandler(
            UUID sessionId, ChatMemory chatMemory, FluxSink<String> sink, MonoSink<Void> monoSink,
            _MessageService messageService, _AuditService auditService) {
        super(sink, monoSink);
        this.sessionId = sessionId;
        this.chatMemory = chatMemory;
        this.messageService = messageService;
        this.auditService = auditService;
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        AiMessage aiMsg = completeResponse.aiMessage();
        chatMemory.add(aiMsg);

        auditService.recordInteraction(sessionId, _InteractionType.AI_COMPLETE_RESULT, aiMsg.text(), null)
                .then(messageService.saveAiResponse(sessionId, aiMsg.text()))
                .then(Mono.fromRunnable(monoSink::success))
                .doOnError(monoSink::error)
                .subscribe();
    }
}
