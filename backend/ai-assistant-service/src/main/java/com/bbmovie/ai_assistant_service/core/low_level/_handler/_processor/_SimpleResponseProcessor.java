package com.bbmovie.ai_assistant_service.core.low_level._handler._processor;

import com.bbmovie.ai_assistant_service.core.low_level._dto._AuditRecord;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._utils._MetricsUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
public class _SimpleResponseProcessor implements _ResponseProcessor {

    private final UUID sessionId;
    private final ChatMemory chatMemory;
    private final _AuditService auditService;
    private final _MessageService messageService;

    private _SimpleResponseProcessor(Builder builder) {
        this.sessionId = builder.sessionId;
        this.chatMemory = builder.chatMemory;
        this.auditService = builder.auditService;
        this.messageService = builder.messageService;
    }

    @Override
    public Mono<Void> process(AiMessage aiMessage, long latency, ChatResponseMetadata metadata) {
        chatMemory.add(aiMessage);

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

        return Mono.when(
                auditMono.onErrorResume(e -> {
                    log.error("Audit failed for simple response but continuing", e);
                    return Mono.empty();
                }),
                saveMono
        );
    }

    public static class Builder {
        private UUID sessionId;
        private ChatMemory chatMemory;
        private _AuditService auditService;
        private _MessageService messageService;

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

        public _SimpleResponseProcessor build() {
            return new _SimpleResponseProcessor(this);
        }
    }
}
