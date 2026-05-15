package bbmovie.ai_platform.agentic_ai.service.chat;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import bbmovie.ai_platform.agentic_ai.service.personalize.PersonalizationService;
import bbmovie.ai_platform.agentic_ai.service.tool.ToolManager;
import bbmovie.ai_platform.agentic_ai.service.chat.advisors.ThinkingAdvisor;
import bbmovie.ai_platform.agentic_ai.service.chat.advisors.TokenUsageAdvisor;
import bbmovie.ai_platform.agentic_ai.service.chat.options.ChatOptionsStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Factory responsible for building complex ChatClient requests.
 * Handles mode-specific options, model capability validation, tool injection,
 * and personalization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRequestFactory {

    private final PersonalizationService personalizationService;
    private final ContentRoutingService contentRoutingService;
    private final ToolManager toolManager;
    private final ModelRoutingService modelRoutingService;
    private final ThinkingAdvisor thinkingAdvisor;
    private final TokenUsageAdvisor tokenUsageAdvisor;
    private final ChatMemory chatMemory;
    private final List<ChatOptionsStrategy> optionsStrategies;


    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    private static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat_memory_retrieve_size";

    /**
     * Creates a fully configured ChatClientRequestSpec.
     * 
     * The process follows these steps:
     * 1. Validate requested Model and Mode (e.g., ensure thinking is supported).
     * 2. Concurrently fetch user personalization and refined asset content.
     * 3. Select the appropriate ChatModel via ModelRoutingService.
     * 4. Build a ChatClient with an Advisor Chain:
     *    - ThinkingAdvisor: Extracts reasoning before memory is saved.
     *    - MessageChatMemoryAdvisor: Persists the conversation.
     * 5. Set system prompt parameters and provider-specific ChatOptions.
     */
    public Mono<ChatClient.ChatClientRequestSpec> createRequest(
            UUID sessionId, UUID userId,
            String message, UUID assetId, AiMode mode, AiModel model) {

        // 1. Fail Fast: Validate Model & Mode
        if (model == null || mode == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model and Mode are required."));
        }

        if (mode == AiMode.THINKING && !model.isSupportsThinking()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model " + model.name() + " does not support Thinking."));
        }

        // 2. Fetch Personalization and Asset Content (via Intelligent Routing)
        Mono<String> assetContentMono = assetId == null ? Mono.just("No file attached.")
                : contentRoutingService.getRefinedContent(assetId);

        return Mono.zip(personalizationService.getPersonalizedContext(userId, sessionId), assetContentMono)
                .map(tuple -> {
                    String personaBrief = tuple.getT1();
                    String fileContent = tuple.getT2();

                    // 3. Select Model and Build Specific Client
                    ChatModel chatModel = modelRoutingService.getModel(model.getProvider());
                    
                    // 4. Build Provider-Specific Options
                    ChatOptions.Builder<?> options = buildProviderOptions(model, mode);

                    String conversationId = userId.toString() + ":" + sessionId.toString();

                    ChatClient.ChatClientRequestSpec spec = ChatClient.builder(chatModel)
                            .build()
                            .prompt()
                            .system(s -> s
                                    .param("user_context", personaBrief)
                                    .param("file_context", fileContent)
                                    .param("current_date", OffsetDateTime.now())
                                    .param("user_id", userId.toString())
                            )
                            .user(message)
                            .options(options)
                            .advisors(a -> a
                                .advisors(
                                    // 1. Extract thinking tags FIRST
                                    thinkingAdvisor, 
                                    // 2. Save to memory (so it includes the extracted 'think' metadata)
                                    MessageChatMemoryAdvisor.builder(chatMemory).build(),
                                    // 3. Record token usage LAST (captures final state)
                                    tokenUsageAdvisor
                                )
                                .param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                                .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 20)
                            )
                            .toolContext(Map.of("userId", userId));

                    return spec;
                })
                .map(spec -> {
                    if (model.isSupportsTools()) {
                        spec.toolCallbacks(toolManager.getAllTools());
                    }
                    return spec;
                });
    }

    private ChatOptions.Builder<?> buildProviderOptions(AiModel model, AiMode mode) {
        return optionsStrategies.stream()
                .filter(s -> s.supports(model.getProvider()))
                .findFirst()
                .map(s -> s.buildOptions(model, mode))
                .orElseThrow(() -> new IllegalArgumentException("Provider not supported: " + model.getProvider()));
    }
}
