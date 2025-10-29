package com.bbmovie.ai_assistant_service.core.low_level._assistant;

import com.bbmovie.ai_assistant_service.core.low_level._tool._AiTool;
import com.bbmovie.ai_assistant_service.core.low_level._utils._AiPersonal;
import com.bbmovie.ai_assistant_service.core.low_level._utils._PromptLoader;
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
import com.bbmovie.ai_assistant_service.core.low_level._database._ChatHistory;
import com.bbmovie.ai_assistant_service.core.low_level._database._ChatHistoryRepository;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Qualifier("_AdminAssistant")
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
            @Qualifier("_StreamingChatModel") StreamingChatModel chatModel,
            @Qualifier("_ChatMemoryProvider") ChatMemoryProvider chatMemoryProvider,
            @Qualifier("_AdminTool") List<_AiTool> toolBeans,
            @Qualifier("_ChatHistoryRepository") _ChatHistoryRepository chatHistoryRepository) { // <-- Inject all beans that implement AiTool
        this.chatModel = chatModel;
        this.chatMemoryProvider = chatMemoryProvider;
        this.chatHistoryRepository = chatHistoryRepository;
        this.systemPrompt = _PromptLoader.loadSystemPrompt(true, _AiPersonal.LLAMA3, null);
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

                    return Flux.create((FluxSink<String> sink) -> {
                        // The processChat method now returns a Mono<Void>
                        // We must subscribe to it to kick off the work.
                        processChat(sessionId, request, sink)
                                .doOnError(sink::error) // Pass errors to the sink
                                .doOnSuccess(v -> sink.complete()) // Complete the sink on success
                                .subscribeOn(Schedulers.boundedElastic()) // Run on a non-blocking thread
                                .subscribe();
                    });
                })
                .doOnError(ex -> log.error("[streaming] Error in chat pipeline for session={}: {}",
                        sessionId, ex.getMessage(), ex)
                );
    }

    /**
     * PROCESSES THE CHAT AND RETURNS A MONO THAT COMPLETES WHEN ALL WORK IS DONE.
     * This method is now fully reactive and recursive.
     */
    private Mono<Void> processChat(String sessionId, ChatRequest chatRequest, FluxSink<String> sink) {
        // We use Mono.create to bridge the callback-based handler with our reactive chain
        return Mono.create(monoSink -> chatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                sink.next(partialResponse); // Stream partials to the client
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                AiMessage aiMsg = completeResponse.aiMessage();
                ChatMemory chatMemory = chatMemoryProvider.get(sessionId);

                if (aiMsg.toolExecutionRequests() != null && !aiMsg.toolExecutionRequests().isEmpty()) {
                    // --- TOOL CALLING LOGIC ---
                    chatMemory.add(aiMsg);
                    String content = aiMsg.text() + " " + aiMsg.toolExecutionRequests().toString();

                    // 1. Create a chain of save-and-execute logic for ALL tools
                    // Flux.fromIterable allows us to process each tool request reactively
                    Flux.fromIterable(aiMsg.toolExecutionRequests())
                            .concatMap(req ->
                                    // This inner chain runs for each tool request, one after another
                                    saveHistory(sessionId, "AI_TOOL_REQUEST", content)
                                            .then(executeAndSaveTool(sessionId, req, chatMemory))
                            )
                            .collectList() // Wait for all tools to execute
                            .flatMap(toolResults -> {
                                // 2. Now that all tools are done, re-prompt the model
                                List<ChatMessage> newMessages = new ArrayList<>();
                                boolean hasSystemPrompt = chatMemory.messages()
                                        .stream()
                                        .anyMatch(m -> m instanceof SystemMessage);
                                if (!hasSystemPrompt) {
                                    newMessages.add(systemPrompt);
                                }
                                newMessages.addAll(chatMemory.messages());

                                ChatRequest afterToolRequest = ChatRequest.builder()
                                        .messages(newMessages)
                                        .toolSpecifications(toolSpecifications)
                                        .build();

                                // 3. RECURSION: Call processChat again
                                //    This returns a Mono<Void>
                                return processChat(sessionId, afterToolRequest, sink);
                            })
                            .then(Mono.fromRunnable(monoSink::success))
                            .doOnError(monoSink::error) // Explicitly pass errors
                            .subscribe(); // Subscribe to kick off this inner chain

                } else {
                    // --- NORMAL AI RESPONSE ---
                    chatMemory.add(aiMsg);
                    saveHistory(sessionId, "AI_RESPONSE", aiMsg.text())
                            .then(Mono.fromRunnable(monoSink::success))
                            .doOnError(monoSink::error) // Explicitly pass errors
                            .subscribe(); // Subscribe to kick off this inner chain
                }
            }

            @Override
            public void onError(Throwable error) {
                log.error("[streaming] Error during streaming", error);
                monoSink.error(error); // Propagate the error
            }
        }));
    }

    /**
     * Helper method to execute a tool and save its results.
     * Returns a Mono<ToolExecutionResultMessage>
     */
    private Mono<ToolExecutionResultMessage> executeAndSaveTool(String sessionId, ToolExecutionRequest req, ChatMemory chatMemory) {
        log.info("[tool] Model requested tool={} args={}", req.name(), req.arguments());
        ToolExecutor toolExecutor = toolExecutors.get(req.name());

        Mono<ToolExecutionResultMessage> toolResultMono;

        if (toolExecutor == null) {
            log.error("[tool] Could not find executor for tool={}", req.name());
            String error = "Tool '" + req.name() + "' not found.";
            toolResultMono = Mono.just(ToolExecutionResultMessage.from(req, error))
                    .flatMap(result -> saveHistory(sessionId, "TOOL_ERROR", error)
                    .thenReturn(result)); // Return the result after saving
        } else {
            // Execute the tool and save the result
            try {
                String executionResult = toolExecutor.execute(req, sessionId);
                toolResultMono = Mono.just(ToolExecutionResultMessage.from(req, executionResult))
                        .flatMap(result -> saveHistory(sessionId, "TOOL_RESULT", executionResult)
                        .thenReturn(result)); // Return the result after saving
                log.info("[tool] Tool '{}' executed: {}", req.name(), executionResult);
            } catch (Exception e) {
                log.error("[tool] Error executing tool '{}': {}", req.name(), e.getMessage(), e);
                String error = "Error executing tool '" + req.name() + "': " + e.getMessage();
                toolResultMono = Mono.just(ToolExecutionResultMessage.from(req, error))
                        .flatMap(result -> saveHistory(sessionId, "TOOL_ERROR", error)
                        .thenReturn(result));
            }
        }

        // Add the result to memory *after* it's been processed and saved
        return toolResultMono.doOnNext(chatMemory::add);
    }

    /**
     * Helper method to save chat history and return a Mono<Void>
     */
    private Mono<Void> saveHistory(String sessionId, String messageType, String content) {
        return chatHistoryRepository.save(createChatHistory(sessionId, messageType, content, Instant.now()))
                .log() // Log the save operation
                .then(); // Convert to Mono<Void>
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