package bbmovie.ai_platform.agentic_ai.service.chat;

import bbmovie.ai_platform.agentic_ai.entity.enums.AiMode;
import bbmovie.ai_platform.agentic_ai.entity.enums.AiModel;
import bbmovie.ai_platform.agentic_ai.service.ContentRoutingService;
import bbmovie.ai_platform.agentic_ai.service.ToolManager;
import bbmovie.ai_platform.agentic_ai.service.personalize.PersonalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import bbmovie.ai_platform.agentic_ai.service.ModelRoutingService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.ThinkOption;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
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

    /**
     * Creates a fully configured ChatClientRequestSpec.
     */
    public Mono<ChatClient.ChatClientRequestSpec> createRequest(
            UUID sessionId, UUID userId,
            String message, UUID assetId, AiMode mode, AiModel model) {

        // 1. Fail Fast: Validate Model & Mode
        if (model == null || mode == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Model and Mode are required."));
        }

        if (mode == AiMode.THINKING && !model.isSupportsThinking()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Model " + model.name() + " does not support Thinking."));
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
                    ChatClient chatClient = ChatClient.create(chatModel);

                    // 4. Build Provider-Specific Options
                    ChatOptions.Builder<?> finalOptions = buildProviderOptions(model, mode);

                    ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                            .system(s -> s
                                    .param("user_context", personaBrief)
                                    .param("file_context", fileContent)
                                    .param("current_date", OffsetDateTime.now())
                                    .param("user_id", userId.toString()))
                            .user(message)
                            .options(finalOptions)
                            .advisors(a -> a
                                    .param("chat_memory_conversation_id", userId.toString() + ":" + sessionId.toString())
                                    .param("chat_memory_retrieve_size", 20))
                            .toolContext(Map.of("userId", userId));

                    if (model.isSupportsTools()) {
                        spec.tools(toolManager.getAllTools());
                    }

                    return spec;
                });
    }

    private ChatOptions.Builder<?> buildProviderOptions(AiModel model, AiMode mode) {
        return switch (model.getProvider()) {
            case "ollama" -> {
                OllamaChatOptions.Builder builder = OllamaChatOptions.builder().model(model.getValue());
                if (mode == AiMode.THINKING) {
                    builder.thinkOption(ThinkOption.ThinkBoolean.ENABLED).temperature(0.3);
                } else {
                    builder.thinkOption(ThinkOption.ThinkBoolean.DISABLED).temperature(0.7);
                }
                yield builder;
            }

            case "google" -> GoogleGenAiChatOptions.builder()
                .model(model.getValue())
                .temperature(mode == AiMode.THINKING ? 0.3 : 0.7);
    
            case "groq", "openai" -> OpenAiChatOptions.builder()
                    .model(model.getValue())
                    .temperature(mode == AiMode.THINKING ? 0.3 : 0.7);
                    
            default -> throw new IllegalArgumentException("Provider không được hỗ trợ");
        };
    }
}
