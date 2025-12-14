package com.bbmovie.ai_assistant_service.handler;

import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import com.bbmovie.ai_assistant_service.dto.Metrics;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.exception.RequiresApprovalException;
import com.bbmovie.ai_assistant_service.handler.processor.ResponseProcessor;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;
import reactor.core.publisher.Mono;

import java.util.UUID;

@SuperBuilder
public class ToolResponseHandler extends BaseResponseHandler {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(ToolResponseHandler.class);

    @NonNull private final ResponseProcessor simpleProcessor;
    @NonNull private final ResponseProcessor toolProcessor;
    private final long requestStartTime;
    @NonNull private final AuditService auditService;
    @NonNull private final UUID sessionId;

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        long latency = System.currentTimeMillis() - requestStartTime;
        AiMessage aiMsg = completeResponse.aiMessage();

        // Handle thinking (logs for audit, optionally emits to clients)
        handleThinking(completeResponse);

        ResponseProcessor processor = (aiMsg.toolExecutionRequests() != null && !aiMsg.toolExecutionRequests().isEmpty())
                ? toolProcessor
                : simpleProcessor;

        processor.process(aiMsg, latency, completeResponse.metadata())
                .then(Mono.fromRunnable(monoSink::success))
                .onErrorResume(RequiresApprovalException.class, ex -> {
                    log.info("HITL Approval Required: {}", ex.getMessage());
                    sink.next(ChatStreamChunk.approvalRequired(
                            ex.getRequestId(),
                            ex.getActionType().name(),
                            ex.getRiskLevel().name(),
                            ex.getDescription()
                    ));
                    monoSink.success();
                    return Mono.empty();
                })
                .doOnError(monoSink::error)
                .subscribe();
    }

    @Override
    public void onError(Throwable error) {
        long latency = System.currentTimeMillis() - requestStartTime;

        Metrics metrics = Metrics.builder()
                .latencyMs(latency)
                .build();
        AuditRecord auditRecord = AuditRecord.builder()
                .sessionId(sessionId)
                .type(InteractionType.ERROR)
                .details(error.getMessage())
                .metrics(metrics)
                .build();

        auditService.recordInteraction(auditRecord).subscribe();

        super.onError(error);
    }
}