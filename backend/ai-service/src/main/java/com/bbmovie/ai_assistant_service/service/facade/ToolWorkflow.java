package com.bbmovie.ai_assistant_service.service.facade;

import com.bbmovie.ai_assistant_service.config.ai.ModelFactory;
import com.bbmovie.ai_assistant_service.config.tool.ToolsRegistry;
import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import com.bbmovie.ai_assistant_service.dto.Metrics;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.dto.response.RagMovieDto;
import com.bbmovie.ai_assistant_service.dto.response.RagRetrievalResult;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.handler.ToolResponseHandler;
import com.bbmovie.ai_assistant_service.handler.processor.SimpleResponseProcessor;
import com.bbmovie.ai_assistant_service.handler.processor.ToolResponseProcessor;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.service.MessageService;
import com.bbmovie.ai_assistant_service.service.ToolExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bbmovie.ai_assistant_service.utils.MetricsUtil;
import com.bbmovie.ai_assistant_service.utils.log.Logger;
import com.bbmovie.ai_assistant_service.utils.log.LoggerFactory;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ToolWorkflow {

    private static final Logger log = LoggerFactory.getLogger(ToolWorkflow.class);

    private final ToolExecutionService toolExecutionService;
    private final ModelFactory modelFactory;
    private final AuditService auditService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ToolWorkflow(
            ToolExecutionService toolExecutionService, ModelFactory modelFactory,
            AuditService auditService, MessageService messageService, ObjectMapper objectMapper) {
        this.toolExecutionService = toolExecutionService;
        this.modelFactory = modelFactory;
        this.auditService = auditService;
        this.messageService = messageService;
        this.objectMapper = objectMapper;
    }

    public Mono<Void> executeWorkflow(
            UUID sessionId,
            AiMode aiMode,
            AiMessage aiMessage,
            ChatMemory chatMemory,
            ToolsRegistry toolRegistry,
            SystemMessage systemPrompt,
            FluxSink<ChatStreamChunk> sink,
            long requestStartTime
    ) {
        // Add the AI message (with tool requests) to memory
        try {
            chatMemory.add(aiMessage);
        } catch (Exception e) {
            log.error("Failed to add AI message to chat memory: {}", e.getMessage());
        }

        long latency = System.currentTimeMillis() - requestStartTime;
        List<ToolExecutionRequest> toolExecutionRequests = aiMessage.toolExecutionRequests();
        // Latency here is for the initial AI response that requested tools
        Metrics metrics = MetricsUtil.getChatMetrics(latency, null, toolExecutionRequests);

        AuditRecord auditRecord = AuditRecord.builder()
                .sessionId(sessionId)
                .type(InteractionType.TOOL_EXECUTION_REQUEST)
                .details(toolExecutionRequests)
                .metrics(metrics)
                .build();

        return auditService.recordInteraction(auditRecord)
                .thenMany(Flux.fromIterable(aiMessage.toolExecutionRequests()))
                .concatMap(req -> toolExecutionService.execute(sessionId, req, toolRegistry, chatMemory))
                .collectList()
                .flatMap(toolResults -> {
                    // Extract RAG results from tool execution results
                    List<RagMovieDto> ragResults = extractRagResults(toolResults);

                    // DEFENSIVE CHECK: Handle cases where the AI hallucinates tool usage
                    // for a handler that was not configured with tools.
                    if (toolRegistry == null) {
                        log.warn("AI attempted to use tools, but no tool registry is configured. Session: {}", sessionId);
                        // Inform the AI that no tools are available and re-prompt.
                        return Flux.fromIterable(aiMessage.toolExecutionRequests())
                                .map(req -> ToolExecutionResultMessage.from(req, "Error: No tools are available in the current context."))
                                .doOnNext(chatMemory::add)
                                .then(Mono.defer(() -> callModelAfterToolRequest(sessionId, aiMode, chatMemory, null, systemPrompt, sink, ragResults)));
                    }

                    return callModelAfterToolRequest(sessionId, aiMode, chatMemory, toolRegistry, systemPrompt, sink, ragResults);
                });
    }

    private Mono<Void> callModelAfterToolRequest(
            UUID sessionId,
            AiMode aiMode,
            ChatMemory chatMemory,
            ToolsRegistry toolRegistry,
            SystemMessage systemPrompt,
            FluxSink<ChatStreamChunk> sink,
            List<RagMovieDto> ragResults
    ) {
        List<ChatMessage> newMessages = new ArrayList<>();
        if (chatMemory.messages().stream().noneMatch(m -> m instanceof SystemMessage)) {
            newMessages.add(systemPrompt);
        }
        newMessages.addAll(chatMemory.messages());

        ChatRequest.Builder builder = ChatRequest.builder()
                .messages(newMessages);

        if (toolRegistry != null) {
            builder.toolSpecifications(toolRegistry.getToolSpecifications());
        }

        ChatRequest afterToolRequest = builder.build();

        // Recursive call to the model
        return Mono.create(recursiveSink -> {
            SimpleResponseProcessor simpleProcessor = SimpleResponseProcessor.builder()
                    .sessionId(sessionId)
                    .chatMemory(chatMemory)
                    .auditService(auditService)
                    .messageService(messageService)
                    .sink(sink)
                    .ragResults(ragResults)
                    .build();

            ToolResponseProcessor toolProcessor = ToolResponseProcessor.builder()
                    .sessionId(sessionId)
                    .aiMode(aiMode)
                    .chatMemory(chatMemory)
                    .toolRegistry(toolRegistry)
                    .systemPrompt(systemPrompt)
                    .toolWorkflowFacade(this)
                    .sink(sink)
                    .requestStartTime(System.currentTimeMillis())
                    .build();
            ToolResponseHandler handler = ToolResponseHandler.builder()
                    .sink(sink)
                    .monoSink(recursiveSink)
                    .simpleProcessor(simpleProcessor)
                    .toolProcessor(toolProcessor)
                    .requestStartTime(System.currentTimeMillis())
                    .auditService(auditService)
                    .sessionId(sessionId)
                    .build();

            modelFactory.getModel(aiMode).chat(afterToolRequest, handler);
        });
    }

    /**
     * Extracts RAG results from tool execution results.
     * Looks for results from the "retrieve_rag_movies" tool and parses the _RagRetrievalResult.
     */
    private List<RagMovieDto> extractRagResults(List<ToolExecutionResultMessage> toolResults) {
        List<RagMovieDto> ragMovies = new ArrayList<>();
        for (ToolExecutionResultMessage result : toolResults) {
            if ("retrieve_rag_movies".equals(result.toolName())) {
                try {
                    // The tool result is a JSON string representation of _RagRetrievalResult
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