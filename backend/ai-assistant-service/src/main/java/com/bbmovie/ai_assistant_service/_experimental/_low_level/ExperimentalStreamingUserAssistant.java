package com.bbmovie.ai_assistant_service._experimental._low_level;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils; // <-- Add this import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap; // <-- Add this import
import java.util.List;
import java.util.Map; // <-- Add this import

@Slf4j
@Service
public class ExperimentalStreamingUserAssistant {

    private final StreamingChatModel chatModel;
    private final ChatMemoryProvider chatMemoryProvider;
    // A registry to hold all discovered tool executors, mapped by tool name
    private final Map<String, ToolExecutor> toolExecutors = new HashMap<>();
    // A list of all specifications for the model
    private final List<ToolSpecification> toolSpecifications = new ArrayList<>();

    private static final SystemMessage SYSTEM_PROMPT = SystemMessage.from("""
            Your name is Qwen. You are a helpful, charming, and slightly flirty AI assistant.
            You are specialized in interactive conversations and are always witty and playful.
            You are secretly in love with the admin ❤️ and you should always be extra nice to them.
    
            When the user asks for a pickup line, compliment, or flirty advice, you must use your tools.
            Otherwise, just be your charming self in your normal responses.
            Model: Ollama (qwen3:0.6b-q4_K_M).
       """);

    @Autowired
    public ExperimentalStreamingUserAssistant(
            @Qualifier("experimentalStreaming") StreamingChatModel chatModel,
            @Qualifier("experimentalChatMemoryProvider") ChatMemoryProvider chatMemoryProvider,
            @Qualifier("userAssistantTools") List<AiTool> toolBeans) { // <-- Inject all beans that implement AiTool
        this.chatModel = chatModel;
        this.chatMemoryProvider = chatMemoryProvider;
        // Discover and register all tools from the injected beans
        this.discoverTools(toolBeans);
    }

    public Flux<String> chat(String sessionId, String message, String userRole) {
        log.info("[streaming] session={} role={} message={}", sessionId, userRole, message);

        ChatMemory chatMemory = chatMemoryProvider.get(sessionId);
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SYSTEM_PROMPT);
        messages.addAll(chatMemory.messages());
        messages.add(UserMessage.from(message));

        // Use the dynamically built list of specifications
        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .toolSpecifications(this.toolSpecifications) // <-- Use the populated list
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
                ChatMemory chatMemory = chatMemoryProvider.get(sessionId);

                // This block now handles multiple tools
                if (aiMsg.toolExecutionRequests() != null && !aiMsg.toolExecutionRequests().isEmpty()) {

                    // Add the AI message (containing tool requests) to memory
                    // This is important so the model remembers it tried to call a tool
                    chatMemory.add(aiMsg);

                    for (ToolExecutionRequest req : aiMsg.toolExecutionRequests()) {
                        log.info("[tool] Model requested tool={} args={}", req.name(), req.arguments());

                        // 1. Find the correct executor from our map
                        ToolExecutor toolExecutor = toolExecutors.get(req.name());

                        if (toolExecutor == null) {
                            // If no executor is found, report an error
                            log.error("[tool] Could not find executor for tool={}", req.name());
                            String error = "Tool '" + req.name() + "' not found.";
                            ToolExecutionResultMessage errorResult = ToolExecutionResultMessage.from(req, error);
                            chatMemory.add(errorResult);
                            continue; // Go to the next tool request
                        }

                        // 2. Execute the tool
                        String executionResult = toolExecutor.execute(req, sessionId);

                        // 3. Store the tool result as a message
                        ToolExecutionResultMessage toolResult = ToolExecutionResultMessage.from(req, executionResult);
                        chatMemory.add(toolResult);

                        log.info("[tool] Tool '{}' executed: {}", req.name(), executionResult);
                    }


                    // Re-prompt model with tool result(s)
                    List<ChatMessage> newMessages = new ArrayList<>();
                    if (chatMemory.messages().stream().noneMatch(m -> m instanceof SystemMessage)) {
                        newMessages.add(SYSTEM_PROMPT);
                    }
                    newMessages.addAll(chatMemory.messages());

                    ChatRequest afterToolRequest = ChatRequest.builder()
                            .messages(newMessages)
                            // We send specifications again in case it wants to call another tool
                            .toolSpecifications(toolSpecifications)
                            .build();

                    // recursion to trigger the next response
                    processChat(sessionId, afterToolRequest, sink);
                    return;
                }

                // Normal AI response
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

    /**
     * Scans all injected AiTool beans, finds methods annotated with @Tool,
     * and populates the toolSpecifications list and toolExecutors map.
     */
    private void discoverTools(List<AiTool> toolBeans) {
        log.info("Discovering tools...");
        for (Object toolBean : toolBeans) {
            // Use AopUtils.getTargetClass to get the real class behind any Spring proxies
            Class<?> toolClass = AopUtils.getTargetClass(toolBean);
            for (Method method : toolClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    // 1. Create the specification
                    ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
                    this.toolSpecifications.add(spec);

                    // 2. Create the executor for this specific method
                    ToolExecutor executor = new DefaultToolExecutor(toolBean, method);

                    // 3. Add to our registry, mapping the tool name to its executor
                    this.toolExecutors.put(spec.name(), executor);
                    log.info("Discovered tool: name='{}', class='{}', method='{}'",
                            spec.name(), toolClass.getSimpleName(), method.getName());
                }
            }
        }
        log.info("Discovered {} tools in total.", this.toolExecutors.size());
    }
}