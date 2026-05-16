package bbmovie.ai_platform.agentic_ai.service.chat.advisors;

import bbmovie.ai_platform.agentic_ai.dto.ConversationId;
import bbmovie.ai_platform.agentic_ai.service.chat.UsageEvent;
import bbmovie.ai_platform.agentic_ai.utils.AiConstants;
import io.nats.client.JetStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import tools.jackson.databind.ObjectMapper;

/**
 * TokenUsageAdvisor extracts token usage metrics from AI responses and 
 * publishes them asynchronously to NATS.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUsageAdvisor implements CallAdvisor, StreamAdvisor {

    private final JetStream jetStream;
    private final ObjectMapper objectMapper;

    @Value("${nats.usage.subject:ai.usage.recorded}")
    private String usageSubject;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        handleUsage(request, response);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return chain.nextStream(request)
                .doOnNext(response -> handleUsage(request, response));
    }

    private void handleUsage(ChatClientRequest request, ChatClientResponse response) {
        if (response == null || response.chatResponse() == null) return;
        
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse.getMetadata() == null || chatResponse.getMetadata().getUsage() == null) return;

        Usage usage = chatResponse.getMetadata().getUsage();
        
        // We only care about final usage (streaming usually has usage in the last chunk)
        if (usage.getPromptTokens() == null || usage.getCompletionTokens() == null) return;
        
        // Noise reduction: Only publish if there is actual usage
        if (usage.getTotalTokens() != null && usage.getTotalTokens() <= 0) return;

        try {
            String conversationIdStr = (String) request.context().get(AiConstants.CHAT_MEMORY_CONVERSATION_ID_KEY);
            if (conversationIdStr == null) return;

            ConversationId convId = ConversationId.of(conversationIdStr);
            String model = chatResponse.getMetadata().getModel();

            UsageEvent event = UsageEvent.of(
                convId.userId(), 
                convId.sessionId(), 
                model, 
                usage.getPromptTokens(), 
                usage.getCompletionTokens()
            );

            log.debug("[Usage] Publishing usage event for session: {}. Tokens: {}", convId.sessionId(), event.totalTokens());
            jetStream.publishAsync(usageSubject, objectMapper.writeValueAsBytes(event));
            
        } catch (Exception e) {
            log.error("[Usage] Failed to publish usage event", e);
        }
    }

    @Override
    public int getOrder() {
        // Run late in the chain to ensure we capture the final response state
        return 100;
    }

    @Override
    public String getName() {
        return "TokenUsageAdvisor";
    }
}
