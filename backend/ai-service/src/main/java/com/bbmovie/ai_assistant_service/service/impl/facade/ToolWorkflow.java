package com.bbmovie.ai_assistant_service.service.impl.facade;

import com.bbmovie.ai_assistant_service.config.ai.ModelFactory;
import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import com.bbmovie.ai_assistant_service.dto.Metrics;
import com.bbmovie.ai_assistant_service.dto.ToolExecutionContext;
import com.bbmovie.ai_assistant_service.dto.response.RagMovieDto;
import com.bbmovie.ai_assistant_service.dto.response.RagRetrievalResult;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.handler.ToolResponseHandler;
import com.bbmovie.ai_assistant_service.handler.processor.SimpleResponseProcessor;
import com.bbmovie.ai_assistant_service.handler.processor.ToolResponseProcessor;
import com.bbmovie.ai_assistant_service.hitl.ExecutionContext;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.service.MessageService;
import com.bbmovie.ai_assistant_service.service.RagService;
import com.bbmovie.ai_assistant_service.service.ToolExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bbmovie.ai_assistant_service.utils.MetricsUtil;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolWorkflow {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(ToolWorkflow.class);

    private final ToolExecutionService toolExecutionService;
    private final ModelFactory modelFactory;
    private final AuditService auditService;
    private final MessageService messageService;
    private final RagService ragService;
    private final ObjectMapper objectMapper;

    public Mono<Void> execute(ToolExecutionContext context) {
        // Add the AI message (with tool requests) to memory
        try {
            context.getChatMemory().add(context.getAiMessage());
        } catch (Exception e) {
            log.error("Failed to add AI message to chat memory: {}", e.getMessage());
        }

        long latency = System.currentTimeMillis() - context.getRequestStartTime();
        List<ToolExecutionRequest> toolExecutionRequests = context.getAiMessage().toolExecutionRequests();
        Metrics metrics = MetricsUtil.getChatMetrics(latency, null, toolExecutionRequests);

        AuditRecord auditRecord = AuditRecord.builder()
                .sessionId(context.getSessionId())
                .type(InteractionType.TOOL_EXECUTION_REQUEST)
                .details(toolExecutionRequests)
                .metrics(metrics)
                .build();

        // Build ExecutionContext for HITL
        ExecutionContext hitlContext = ExecutionContext.builder()
                .sessionId(context.getSessionId())
                .userId(context.getUserId())
                .internalApprovalToken(context.getInternalApprovalToken())
                .messageId(context.getMessageId())
                .build();

        return auditService.recordInteraction(auditRecord)
                .thenMany(Flux.fromIterable(context.getAiMessage().toolExecutionRequests()))
                .concatMap(req -> toolExecutionService.execute(
                        context.getSessionId(), 
                        req, 
                        context.getToolRegistry(), 
                        context.getChatMemory(),
                        hitlContext // Pass context
                ))
                .collectList()
                .flatMap(toolResults -> {
                    List<RagMovieDto> ragResults = extractRagResults(toolResults);

                    if (context.getToolRegistry() == null) {
                        log.warn("AI attempted to use tools, but no tool registry is configured. Session: {}", context.getSessionId());
                        return Flux.fromIterable(context.getAiMessage().toolExecutionRequests())
                                .map(req -> ToolExecutionResultMessage.from(req, "Error: No tools are available in the current context."))
                                .doOnNext(context.getChatMemory()::add)
                                .then(Mono.defer(() -> callModelAfterToolRequest(context, ragResults)));
                    }

                    return callModelAfterToolRequest(context, ragResults);
                });
    }

    private Mono<Void> callModelAfterToolRequest(ToolExecutionContext context, List<RagMovieDto> ragResults) {
        List<ChatMessage> newMessages = new ArrayList<>();
        if (context.getChatMemory().messages().stream().noneMatch(m -> m instanceof SystemMessage)) {
            newMessages.add(context.getSystemPrompt());
        }
        newMessages.addAll(context.getChatMemory().messages());

        ChatRequest.Builder builder = ChatRequest.builder()
                .messages(newMessages);

        if (context.getToolRegistry() != null) {
            builder.toolSpecifications(context.getToolRegistry().getToolSpecifications());
        }

        ChatRequest afterToolRequest = builder.build();

        return Mono.create(recursiveSink -> {
            SimpleResponseProcessor simpleProcessor = initializeResponseProcessor(context, ragResults);
            
            // Re-initialize tool processor with HITL context
            ToolResponseProcessor toolProcessor = initializeToolResponseProcessor(context);
            
            ToolResponseHandler handler = initializeResponseHandler(context, recursiveSink, simpleProcessor, toolProcessor);

            modelFactory.getModel(context.getAiMode()).chat(afterToolRequest, handler);
        });
    }

    private ToolResponseHandler initializeResponseHandler(
            ToolExecutionContext context, MonoSink<Void> recursiveSink,
            SimpleResponseProcessor simpleProcessor, ToolResponseProcessor toolProcessor) {
        return ToolResponseHandler.builder()
                .sessionId(context.getSessionId())
                .sink(context.getSink())
                .monoSink(recursiveSink)
                .simpleProcessor(simpleProcessor)
                .toolProcessor(toolProcessor)
                .requestStartTime(System.currentTimeMillis())
                .auditService(auditService)
                .build();
    }

    private ToolResponseProcessor initializeToolResponseProcessor(ToolExecutionContext context) {
        return ToolResponseProcessor.builder()
                .sessionId(context.getSessionId())
                .aiMode(context.getAiMode())
                .chatMemory(context.getChatMemory())
                .toolRegistry(context.getToolRegistry())
                .systemPrompt(context.getSystemPrompt())
                .toolWorkflow(this)
                .sink(context.getSink())
                .requestStartTime(System.currentTimeMillis())
                .userId(context.getUserId())
                .internalApprovalToken(context.getInternalApprovalToken())
                .messageId(context.getMessageId())
                .build();
    }

    private SimpleResponseProcessor initializeResponseProcessor(ToolExecutionContext context, List<RagMovieDto> ragResults) {
        return SimpleResponseProcessor.builder()
                .sessionId(context.getSessionId())
                .chatMemory(context.getChatMemory())
                .auditService(auditService)
                .messageService(messageService)
                .ragService(ragService)
                .sink(context.getSink())
                .ragResults(ragResults)
                .build();
    }
    
    private List<RagMovieDto> extractRagResults(List<ToolExecutionResultMessage> toolResults) {
        List<RagMovieDto> ragMovies = new ArrayList<>();
        for (ToolExecutionResultMessage result : toolResults) {
            if ("retrieve_rag_movies".equals(result.toolName())) {
                try {
                    String resultText = result.text();
                    RagRetrievalResult ragResult = objectMapper.readValue(resultText, RagRetrievalResult.class);
                    if (ragResult.documents() != null && !ragResult.documents().isEmpty()) {
                        ragMovies.addAll(ragResult.documents());
                    }
                } catch (Exception e) {
                    log.warn("[rag] Failed to parse RAG results from tool execution: {}", e.getMessage());
                }
            }
        }
        return ragMovies;
    }
}
