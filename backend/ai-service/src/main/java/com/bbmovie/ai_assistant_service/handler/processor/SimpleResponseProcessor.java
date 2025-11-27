package com.bbmovie.ai_assistant_service.handler.processor;

import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import com.bbmovie.ai_assistant_service.dto.Metrics;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.dto.response.RagMovieDto;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.service.MessageService;
import com.bbmovie.ai_assistant_service.utils.MetricsUtil;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.List;

import java.util.UUID;

@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleResponseProcessor implements ResponseProcessor {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(SimpleResponseProcessor.class);

    @NonNull private final UUID sessionId;
    @NonNull private final ChatMemory chatMemory;
    @NonNull private final AuditService auditService;
    @NonNull private final MessageService messageService;
    private final FluxSink<ChatStreamChunk> sink;
    private final List<RagMovieDto> ragResults;

    @Override
    public Mono<Void> process(AiMessage aiMessage, long latency, ChatResponseMetadata metadata) {
        try {
            chatMemory.add(aiMessage);
        } catch (Exception e) {
            log.error("Failed to add AI message to chat memory: {}", e.getMessage());
        }

        // Note: Thinking is already emitted by _ToolResponseHandler before calling this processor

        Metrics metrics = MetricsUtil.getChatMetrics(latency, metadata, aiMessage.toolExecutionRequests());
        AuditRecord auditRecord = AuditRecord.builder()
                .sessionId(sessionId)
                .type(InteractionType.AI_COMPLETE_RESULT)
                .details(aiMessage.text())
                .metrics(metrics)
                .build();

        Mono<Void> auditMono = auditService.recordInteraction(auditRecord);
        Mono<Void> saveMono = messageService.saveAiResponse(sessionId, aiMessage.text())
                .then();

        // Emit RAG results after content if available
        Mono<Void> ragMono = Mono.fromRunnable(() -> {
            if (ragResults != null && !ragResults.isEmpty() && sink != null) {
                sink.next(ChatStreamChunk.ragResult(ragResults));
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
}
