package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._dto._AuditRecord;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._handler._processor._ResponseProcessor;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.Objects;
import java.util.UUID;

public class _ToolResponseHandler extends _BaseResponseHandler {

    private static final _Logger log = _LoggerFactory.getLogger(_ToolResponseHandler.class);

    private final _ResponseProcessor simpleProcessor;
    private final _ResponseProcessor toolProcessor;
    private final long requestStartTime;
    private final _AuditService auditService;
    private final UUID sessionId;

    private _ToolResponseHandler(Builder builder) {
        super(builder.sink, builder.monoSink);
        this.simpleProcessor = builder.simpleProcessor;
        this.toolProcessor = builder.toolProcessor;
        this.requestStartTime = builder.requestStartTime;
        this.auditService = builder.auditService;
        this.sessionId = builder.sessionId;
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
                .details(error.getMessage())
                .metrics(metrics)
                .build();

        auditService.recordInteraction(auditRecord)
                .subscribe(
                        v -> log.debug("Error audit recorded"),
                        e -> log.error("Failed to record error audit", e)
                );

        super.onError(error);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private FluxSink<String> sink;
        private MonoSink<Void> monoSink;
        private _ResponseProcessor simpleProcessor;
        private _ResponseProcessor toolProcessor;
        private long requestStartTime;
        private _AuditService auditService;
        private UUID sessionId;

        public Builder sink(FluxSink<String> sink) {
            this.sink = sink;
            return this;
        }

        public Builder monoSink(MonoSink<Void> monoSink) {
            this.monoSink = monoSink;
            return this;
        }

        public Builder simpleProcessor(_ResponseProcessor simpleProcessor) {
            this.simpleProcessor = simpleProcessor;
            return this;
        }

        public Builder toolProcessor(_ResponseProcessor toolProcessor) {
            this.toolProcessor = toolProcessor;
            return this;
        }

        public Builder requestStartTime(long requestStartTime) {
            this.requestStartTime = requestStartTime;
            return this;
        }

        public Builder auditService(_AuditService auditService) {
            this.auditService = auditService;
            return this;
        }

        public Builder sessionId(UUID sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public _ToolResponseHandler build() {
            Objects.requireNonNull(sink, "sink must not be null");
            Objects.requireNonNull(monoSink, "monoSink must not be null");
            Objects.requireNonNull(simpleProcessor, "simpleProcessor must not be null");
            Objects.requireNonNull(toolProcessor, "toolProcessor must not be null");
            Objects.requireNonNull(auditService, "auditService must not be null");
            Objects.requireNonNull(sessionId, "sessionId must not be null");
            return new _ToolResponseHandler(this);
        }
    }
}