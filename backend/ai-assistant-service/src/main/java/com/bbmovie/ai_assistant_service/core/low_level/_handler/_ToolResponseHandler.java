package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._dto._AuditRecord;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._handler._processor._ResponseProcessor;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.UUID;

@Slf4j
public class _ToolResponseHandler extends _BaseResponseHandler {

    private final _ResponseProcessor simpleProcessor;
    private final _ResponseProcessor toolProcessor;
    private final long requestStartTime;
    private final _AuditService auditService;
    private final UUID sessionId;

    public _ToolResponseHandler(
            FluxSink<String> sink,
            MonoSink<Void> monoSink,
            _ResponseProcessor simpleProcessor,
            _ResponseProcessor toolProcessor,
            long requestStartTime,
            _AuditService auditService,
            UUID sessionId
    ) {
        super(sink, monoSink);
        this.simpleProcessor = simpleProcessor;
        this.toolProcessor = toolProcessor;
        this.requestStartTime = requestStartTime;
        this.auditService = auditService;
        this.sessionId = sessionId;
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        long latency = System.currentTimeMillis() - requestStartTime;
        AiMessage aiMsg = completeResponse.aiMessage();

        _ResponseProcessor processor = (aiMsg.toolExecutionRequests() != null && !aiMsg.toolExecutionRequests().isEmpty())
                ? toolProcessor
                : simpleProcessor;

        processor.process(aiMsg, latency, completeResponse.metadata())
                .then(Mono.fromRunnable(monoSink::success))
                .doOnError(monoSink::error)
                .subscribe();
    }

    @Override
    public void onError(Throwable error) {
        long latency = System.currentTimeMillis() - requestStartTime;

        _Metrics metrics = _Metrics.builder()
                .latencyMs(latency)
                .build();
        _AuditRecord auditRecord = _AuditRecord.builder()
                .sessionId(sessionId)
                .type(_InteractionType.ERROR)
                .details(error)
                .metrics(metrics)
                .build();

        auditService.recordInteraction(auditRecord)
                .subscribe(
                        v -> log.debug("Error audit recorded"),
                        e -> log.error("Failed to record error audit", e)
                );

        super.onError(error);
    }
}