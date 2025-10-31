package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatMessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._tool._ToolRegistry;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class _ToolExecutingResponseHandler extends _BaseResponseHandler {

    private final UUID sessionId;
    private final ChatMemory chatMemory;
    private final StreamingChatModel chatModel;
    private final SystemMessage systemPrompt;
    private final _ToolRegistry toolRegistry;
    private final _ChatMessageService chatMessageService;
    private final _ToolExecutionService toolExecutionService;
    private final _AuditService auditService;

    public _ToolExecutingResponseHandler(
            UUID sessionId, ChatMemory chatMemory, FluxSink<String> sink, MonoSink<Void> monoSink,
            StreamingChatModel chatModel, SystemMessage systemPrompt, _ToolRegistry toolRegistry,
            _ChatMessageService chatMessageService, _ToolExecutionService toolExecutionService, _AuditService auditService) {
        super(sink, monoSink);
        this.sessionId = sessionId;
        this.chatMemory = chatMemory;
        this.chatModel = chatModel;
        this.systemPrompt = systemPrompt;
        this.toolRegistry = toolRegistry;
        this.chatMessageService = chatMessageService;
        this.toolExecutionService = toolExecutionService;
        this.auditService = auditService;
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        AiMessage aiMsg = completeResponse.aiMessage();

        if (aiMsg.toolExecutionRequests() != null && !aiMsg.toolExecutionRequests().isEmpty()) {
            handleToolExecution(aiMsg);
        } else {
            handleSimpleResponse(aiMsg);
        }
    }

    private void handleSimpleResponse(AiMessage aiMsg) {
        chatMemory.add(aiMsg);
        auditService.recordInteraction(sessionId, _InteractionType.AI_COMPLETE_RESPONSE, aiMsg.text())
                .then(chatMessageService.saveAiResponse(sessionId, aiMsg.text()))
                .then(Mono.fromRunnable(monoSink::success))
                .doOnError(monoSink::error)
                .subscribe();
    }

    private void handleToolExecution(AiMessage aiMsg) {
        chatMemory.add(aiMsg);
        String thinkingContent = aiMsg.text() + " " + aiMsg.toolExecutionRequests().toString();

        auditService.recordInteraction(sessionId, _InteractionType.TOOL_EXECUTION_REQUEST, aiMsg.toolExecutionRequests())
                .then(chatMessageService.saveToolRequest(sessionId, thinkingContent))
                .thenMany(Flux.fromIterable(aiMsg.toolExecutionRequests()))
                .concatMap(req -> toolExecutionService.executeAndSave(sessionId, req, toolRegistry, chatMemory))
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

                    return processRecursively(afterToolRequest);
                })
                .then(Mono.fromRunnable(monoSink::success))
                .doOnError(monoSink::error)
                .subscribe();
    }

    private Mono<Void> processRecursively(ChatRequest chatRequest) {
        return Mono.create(recursiveSink ->
                chatModel.chat(chatRequest, new _ToolExecutingResponseHandler(
                        sessionId, chatMemory, sink, recursiveSink, chatModel, systemPrompt,
                        toolRegistry, chatMessageService, toolExecutionService, auditService
                ))
        );
    }
}