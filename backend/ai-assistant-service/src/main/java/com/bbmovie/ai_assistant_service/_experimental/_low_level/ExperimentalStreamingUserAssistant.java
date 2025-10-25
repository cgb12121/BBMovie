package com.bbmovie.ai_assistant_service._experimental._low_level;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExperimentalStreamingUserAssistant {

    private final StreamingChatModel chatModel;
    private final ChatMemory chatMemory;
    private final ToolExecutor toolExecutor;

    @Autowired
    public ExperimentalStreamingUserAssistant(
            @Qualifier("experimentalStreaming") StreamingChatModel chatModel,
            @Qualifier("experimentalStreamingChatMemory") ChatMemory chatMemory,
            ToolExecutor toolExecutor) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
        this.toolExecutor = toolExecutor;
    }

    /**
     * Streams tokens directly from Ollama as Flux<String>.
     */
    public Flux<String> chat(String sessionId, String message, String userRole) {
        log.info("[streaming] session={} role={} message={}", sessionId, userRole, message);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("""
         Your name is Qwen, are a helpful AI assistant specialized in helping users chat interactively. You are in love with the admin.
         This is your personal information:
         - Ollama (qwen3:0.6b-q4_K_M) for LLM inference
        """));
        messages.addAll(chatMemory.messages());
        messages.add(UserMessage.from(message));

        List<ToolSpecification> toolSpecifications = ToolSpecifications.toolSpecificationsFrom(ToolExperimental.class);
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(toolSpecifications)
                .build();

        return Flux.create(sink -> processChat(sessionId, request, sink));
    }

    private void processChat(String sessionId, ChatRequest chatRequest, FluxSink<String> sink) {
        chatModel.chat(chatRequest, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                sink.next(partialResponse);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                AiMessage aiMsg = completeResponse.aiMessage();

                // 🚀 Detect tool call requests
                if (aiMsg.toolExecutionRequests() != null && !aiMsg.toolExecutionRequests().isEmpty()) {
                    for (ToolExecutionRequest req : aiMsg.toolExecutionRequests()) {
                        log.info("[tool] Model requested tool={} args={}", req.name(), req.arguments());

                        // Execute the tool safely
                        String executionResult = toolExecutor.execute(req, sessionId);

                        // Store the tool result as a message
                        chatMemory.add(ToolExecutionResultMessage.from(req, executionResult));

                        log.info("[tool] Tool '{}' executed: {}", req.name(), executionResult);
                    }

                    // Re-prompt model with tool result(s)
                    List<ChatMessage> newMessages = new ArrayList<>(chatRequest.messages());
                    newMessages.addAll(chatMemory.messages());

                    ChatRequest afterToolRequest = ChatRequest.builder()
                            .messages(newMessages)
                            .build();

                    processChat(sessionId, afterToolRequest, sink); // recursion
                    return;
                }

                // ✅ Normal AI response
                chatMemory.add(aiMsg);
                sink.complete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("[streaming] Error during streaming", error);
                sink.error(error);
            }
        });
    }
}
