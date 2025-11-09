package com.bbmovie.ai_assistant_service.core.low_level._service._facade;

import com.bbmovie.ai_assistant_service.core.low_level._config._ai._ModelFactory;
import com.bbmovie.ai_assistant_service.core.low_level._config._tool._ToolsRegistry;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._handler._ToolResponseHandler;
import com.bbmovie.ai_assistant_service.core.low_level._handler._processor._SimpleResponseProcessor;
import com.bbmovie.ai_assistant_service.core.low_level._handler._processor._ToolResponseProcessor;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._MessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._utils._MetricsUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.request.ChatRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class _ToolWorkflowFacade {

    private final _ToolExecutionService toolExecutionService;
    private final _ModelFactory modelFactory;
    private final _AuditService auditService;
    private final _MessageService messageService;

    public _ToolWorkflowFacade(
            _ToolExecutionService toolExecutionService, _ModelFactory modelFactory,
            _AuditService auditService, _MessageService messageService) {
        this.toolExecutionService = toolExecutionService;
        this.modelFactory = modelFactory;
        this.auditService = auditService;
        this.messageService = messageService;
    }

    public Mono<Void> executeWorkflow(
            UUID sessionId,
            _AiMode aiMode,
            AiMessage aiMessage,
            ChatMemory chatMemory,
            _ToolsRegistry toolRegistry,
            SystemMessage systemPrompt,
            FluxSink<String> sink,
            MonoSink<Void> monoSink,
            long requestStartTime
    ) {
        // Add the AI message (with tool requests) to memory
        chatMemory.add(aiMessage);

        // Metrics for the tool request part
        _Metrics metrics = _MetricsUtil.getChatMetrics(System.currentTimeMillis() - requestStartTime, null, aiMessage.toolExecutionRequests()); // Latency here is for the initial AI response that requested tools

        // Save the tool request message
        String thinkingContent = aiMessage.text() + " " + aiMessage.toolExecutionRequests().toString();

        return auditService.recordInteraction(
                        sessionId,
                        _InteractionType.TOOL_EXECUTION_REQUEST,
                        thinkingContent,
                        metrics
                )
                .thenMany(Flux.fromIterable(aiMessage.toolExecutionRequests()))
                .concatMap(req -> toolExecutionService.execute(sessionId, req, toolRegistry, chatMemory))
                .collectList()
                .flatMap(toolResults -> {
                    List<ChatMessage> newMessages = new ArrayList<>();
                    if (chatMemory.messages().stream().noneMatch(m -> m instanceof SystemMessage)) {
                        newMessages.add(systemPrompt);
                    }
                    newMessages.addAll(chatMemory.messages());

                    ChatRequest afterToolRequest = ChatRequest.builder()
                            .messages(newMessages)
                            .toolSpecifications(toolRegistry.getToolSpecifications())
                            .build();

                    // Recursive call to the model
                    return Mono.create(recursiveSink ->
                            modelFactory.getModel(aiMode).chat(afterToolRequest, new _ToolResponseHandler(
                                    sink,
                                    monoSink,
                                    buildResponseProcessor(sessionId, chatMemory),
                                    buildToolResponseProcessor(
                                            sessionId, aiMode, chatMemory, toolRegistry,
                                            systemPrompt, sink, monoSink, requestStartTime
                                    ),
                                    requestStartTime,
                                    auditService,
                                    sessionId
                            ))
                    );
                });
    }

    private _ToolResponseProcessor buildToolResponseProcessor(
            UUID sessionId, _AiMode aiMode, ChatMemory chatMemory,
            _ToolsRegistry toolRegistry, SystemMessage systemPrompt,
            FluxSink<String> sink, MonoSink<Void> monoSink, long requestStartTime) {
        return new _ToolResponseProcessor.Builder()
                .sessionId(sessionId)
                .aiMode(aiMode)
                .chatMemory(chatMemory)
                .toolRegistry(toolRegistry)
                .systemPrompt(systemPrompt)
                .toolWorkflowFacade(this)
                .sink(sink)
                .monoSink(monoSink)
                .requestStartTime(requestStartTime)
                .build();
    }

    private _SimpleResponseProcessor buildResponseProcessor(UUID sessionId, ChatMemory chatMemory) {
        return new _SimpleResponseProcessor.Builder()
                .sessionId(sessionId)
                .chatMemory(chatMemory)
                .auditService(auditService)
                .messageService(messageService)
                .build();
    }
}
