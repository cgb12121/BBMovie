package com.bbmovie.ai_assistant_service._experimental._assistant;

import com.bbmovie.ai_assistant_service._experimental._tool._AiTool;
import com.bbmovie.ai_assistant_service.utils.prompt._AiPersonal;
import com.bbmovie.ai_assistant_service.utils.prompt._PromptLoader;
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
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import com.bbmovie.ai_assistant_service._experimental._database._ChatHistory;
import com.bbmovie.ai_assistant_service._experimental._database._ChatHistoryRepository;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnBooleanProperty(name = "ai.experimental.enabled")
public class _AdminAssistant {

    private final StreamingChatModel chatModel;
    private final ChatMemoryProvider chatMemoryProvider;
    private final _ChatHistoryRepository chatHistoryRepository;
    // A registry to hold all discovered tool executors, mapped by tool name
    private final Map<String, ToolExecutor> toolExecutors = new HashMap<>();
    // A list of all specifications for the model
    private final List<ToolSpecification> toolSpecifications = new ArrayList<>();

    private final SystemMessage systemPrompt;

    @Autowired
    public _AdminAssistant(
            @Qualifier("experimentalStreaming") StreamingChatModel chatModel,
            @Qualifier("experimentalChatMemoryProvider") ChatMemoryProvider chatMemoryProvider,
            @Qualifier("adminAssistantTools") List<_AiTool> toolBeans,
            @Qualifier("experimentalChatHistoryRepository") _ChatHistoryRepository chatHistoryRepository) { // <-- Inject all beans that implement AiTool
        this.chatModel = chatModel;
        this.chatMemoryProvider = chatMemoryProvider;
        this.chatHistoryRepository = chatHistoryRepository;
        this.systemPrompt = _PromptLoader.loadSystemPrompt(_AiPersonal.QWEN, null);
        // Discover and register all tools from the injected beans
        this.discoverTools(toolBeans);
    }

    /**
     * Scans all injected AiTool beans, finds methods annotated with @Tool,
     * and populates the toolSpecifications list and toolExecutors map.
     * <p>
     * Can use {@link PostConstruct}` to init all allowed tools instead, optional.
     */
    private void discoverTools(List<_AiTool> toolBeans) {
        log.info("Discovering tools...");
        for (Object toolBean : toolBeans) {
            // Use AopUtils.getTargetClass to get the real class behind any Spring proxies
            Class<?> toolClass = AopUtils.getTargetClass(toolBean);
            for (Method method : toolClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    // Create the specification
                    ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
                    this.toolSpecifications.add(spec);

                    // Create the executor for this specific method
                    ToolExecutor executor = new DefaultToolExecutor(toolBean, method);

                    // Add to our registry, mapping the tool name to its executor
                    this.toolExecutors.put(spec.name(), executor);
                    log.info("Discovered tool: name='{}', class='{}', method='{}'",
                            spec.name(), toolClass.getSimpleName(), method.getName());
                }
            }
        }
        log.info("Discovered {} tools in total.", this.toolExecutors.size());
    }

    public Flux<String> chat(String sessionId, String message, String userRole) {
        log.info("[streaming] session={} role={} message={}", sessionId, userRole, message);

        _ChatHistory userMessageHistory = createChatHistory(sessionId, "USER", message, Instant.now());

        return chatHistoryRepository.save(userMessageHistory)
                .log()
                .flatMapMany(savedHistory -> {
                    log.info("[streaming] User message saved (id={}), proceeding to AI chat.", savedHistory.getId());

                    ChatMemory chatMemory = chatMemoryProvider.get(sessionId);
                    List<ChatMessage> messages = new ArrayList<>();
                    messages.add(systemPrompt);
                    messages.addAll(chatMemory.messages());
                    messages.add(UserMessage.from(message));

                    ChatRequest request = ChatRequest.builder()
                            .messages(messages)
                            .toolSpecifications(this.toolSpecifications)
                            .build();

                    return Flux.create((FluxSink<String> sink) -> processChat(sessionId, request, sink));
                })
                .doOnError(ex -> log.error("[streaming] Error in chat pipeline for session={}: {}",
                        sessionId, ex.getMessage(), ex)
                );
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

                if (aiMsg.toolExecutionRequests() != null && !aiMsg.toolExecutionRequests().isEmpty()) {

                    // Add the AI message (containing tool requests) to memory
                    // This is important so the model remembers it tried to call a tool
                    chatMemory.add(aiMsg);

                    String content = aiMsg.text() + " " + aiMsg.toolExecutionRequests().toString();
                    _ChatHistory aiResponse = createChatHistory(sessionId, "AI_TOOL_REQUEST", content, Instant.now());
                    chatHistoryRepository.save(aiResponse).subscribe();

                    for (ToolExecutionRequest req : aiMsg.toolExecutionRequests()) {
                        log.info("[tool] Model requested tool={} args={}", req.name(), req.arguments());

                        ToolExecutor toolExecutor = toolExecutors.get(req.name());

                        if (toolExecutor == null) {
                            log.error("[tool] Could not find executor for tool={}", req.name());
                            String error = "Tool '" + req.name() + "' not found.";
                            ToolExecutionResultMessage errorResult = ToolExecutionResultMessage.from(req, error);
                            chatMemory.add(errorResult);

                            _ChatHistory toolError = createChatHistory(sessionId, "TOOL_ERROR", error, Instant.now());
                            chatHistoryRepository.save(toolError).subscribe();
                            continue;
                        }

                        String executionResult = toolExecutor.execute(req, sessionId);

                        ToolExecutionResultMessage toolResult = ToolExecutionResultMessage.from(req, executionResult);
                        chatMemory.add(toolResult);

                        _ChatHistory toolHistory = createChatHistory(sessionId, "TOOL_REQUEST", req.toString(), Instant.now());
                        chatHistoryRepository.save(toolHistory).subscribe();

                        log.info("[tool] Tool '{}' executed: {}", req.name(), executionResult);
                    }


                    // Re-prompt model with tool result(s)
                    List<ChatMessage> newMessages = new ArrayList<>();

                    boolean hasSystemPrompt = chatMemory.messages()
                            .stream()
                            .anyMatch(m -> m instanceof SystemMessage);
                    if (hasSystemPrompt) {
                        newMessages.add(systemPrompt);
                    }
                    newMessages.addAll(chatMemory.messages());

                    ChatRequest afterToolRequest = ChatRequest.builder()
                            .messages(newMessages)
                            .toolSpecifications(toolSpecifications) // send specifications again in case it wants to call another tool
                            .build();

                    processChat(sessionId, afterToolRequest, sink); // recursion to trigger the next response
                    return;
                }

                // Normal AI response
                chatMemory.add(aiMsg);

                _ChatHistory aiHistory = createChatHistory(sessionId, "AI_RESPONSE", aiMsg.text(), Instant.now());
                chatHistoryRepository.save(aiHistory).subscribe();

                sink.complete();
            }

            @Override
            public void onError(Throwable error) {
                log.error("[streaming] Error during streaming", error);
                sink.error(error);
            }
        });
    }

    private _ChatHistory createChatHistory(String sessionId, String messageType, String content, Instant timestamp) {
        return _ChatHistory.builder()
                .sessionId(sessionId)
                .messageType(messageType)
                .content(content)
                .timestamp(timestamp)
                .build();
    }
}