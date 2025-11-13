package com.bbmovie.ai_assistant_service.core.low_level._handler._processor;

import com.bbmovie.ai_assistant_service.core.low_level._dto._AuditRecord;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._dto._response._RagMovieDto;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._utils._MetricsUtil;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.List;

import java.util.Objects;
import java.util.UUID;

public class _SimpleResponseProcessor implements _ResponseProcessor {

    private static final _Logger log = _LoggerFactory.getLogger(_SimpleResponseProcessor.class);

    private final UUID sessionId;
    private final ChatMemory chatMemory;
    private final _AuditService auditService;
    private final _MessageService messageService;
    private final FluxSink<_ChatStreamChunk> sink;
    private final List<_RagMovieDto> ragResults;

    private _SimpleResponseProcessor(Builder builder) {
        this.sessionId = builder.sessionId;
        this.chatMemory = builder.chatMemory;
        this.auditService = builder.auditService;
        this.messageService = builder.messageService;
        this.sink = builder.sink;
        this.ragResults = builder.ragResults;
    }

    @Override
    public Mono<Void> process(AiMessage aiMessage, long latency, ChatResponseMetadata metadata) {
        try {
            chatMemory.add(aiMessage);
        } catch (Exception e) {
            log.error("Failed to add AI message to chat memory: {}", e.getMessage());
        }

        // Note: Thinking is already emitted by _ToolResponseHandler before calling this processor

        _Metrics metrics = _MetricsUtil.getChatMetrics(latency, metadata, aiMessage.toolExecutionRequests());
        _AuditRecord auditRecord = _AuditRecord.builder()
                .sessionId(sessionId)
                .type(_InteractionType.AI_COMPLETE_RESULT)
                .details(aiMessage.text())
                .metrics(metrics)
                .build();

        Mono<Void> auditMono = auditService.recordInteraction(auditRecord);
        Mono<Void> saveMono = messageService.saveAiResponse(sessionId, aiMessage.text())
                .then();

        // Emit RAG results after content if available
        Mono<Void> ragMono = Mono.fromRunnable(() -> {
            if (ragResults != null && !ragResults.isEmpty() && sink != null) {
                sink.next(_ChatStreamChunk.ragResult(ragResults));
            }
        });

        return Mono.when(
                auditMono.onErrorResume(e -> {
                    log.error("Audit failed for simple response but continuing", e);
                    return Mono.empty();
                }),
                saveMono,
                ragMono
        );
    }

    public static class Builder {
        private UUID sessionId;
        private ChatMemory chatMemory;
        private _AuditService auditService;
        private _MessageService messageService;
        private FluxSink<_ChatStreamChunk> sink;
        private List<_RagMovieDto> ragResults;

        public Builder sessionId(UUID sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder chatMemory(ChatMemory chatMemory) {
            this.chatMemory = chatMemory;
            return this;
        }

        public Builder auditService(_AuditService auditService) {
            this.auditService = auditService;
            return this;
        }

        public Builder messageService(_MessageService messageService) {
            this.messageService = messageService;
            return this;
        }

        public Builder sink(FluxSink<_ChatStreamChunk> sink) {
            this.sink = sink;
            return this;
        }

        public Builder ragResults(List<_RagMovieDto> ragResults) {
            this.ragResults = ragResults;
            return this;
        }

        public _SimpleResponseProcessor build() {
            Objects.requireNonNull(sessionId, "sessionId must not be null");
            Objects.requireNonNull(chatMemory, "chatMemory must not be null");
            Objects.requireNonNull(auditService, "auditService must not be null");
            Objects.requireNonNull(messageService, "messageService must not be null");
            return new _SimpleResponseProcessor(this);
        }
    }
}
