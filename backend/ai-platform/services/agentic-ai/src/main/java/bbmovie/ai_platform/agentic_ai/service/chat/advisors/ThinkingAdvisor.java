package bbmovie.ai_platform.agentic_ai.service.chat.advisors;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ThinkingAdvisor intercepts the AI's response to extract and handle "thinking" blocks.
 * 
 * Many modern models (like DeepSeek-R1 or Qwen-Reasoning) output their internal thought 
 * process wrapped in <think>...</think> tags. This advisor:
 * 1. Locates these tags using a stateful parser for streaming and Regex for unary calls.
 * 2. Extracts the reasoning content.
 * 3. Moves the reasoning content into the message metadata (key: "think").
 * 4. Removes the reasoning tags from the final user-facing content.
 * 
 * This ensures a clean UI experience while still persisting the reasoning process 
 * for auditing or debugging purposes.
 */
@Component
public class ThinkingAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Pattern THINK_PATTERN = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL);

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);
        return processResponse(response);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return Flux.defer(() -> {
            // Per-subscription state for the streaming parser
            StreamingThinkingProcessor processor = new StreamingThinkingProcessor();
            return chain.nextStream(request)
                    .map(processor::process);
        });
    }

    /**
     * Non-streaming response processing using Regex.
     */
    private ChatClientResponse processResponse(ChatClientResponse clientResponse) {
        if (clientResponse == null || clientResponse.chatResponse() == null) {
            return clientResponse;
        }

        ChatResponse chatResponse = clientResponse.chatResponse();
        List<Generation> generations = chatResponse.getResults();

        if (generations == null || generations.isEmpty()) {
            return clientResponse;
        }

        Generation generation = generations.get(0);
        AssistantMessage originalMessage = (AssistantMessage) generation.getOutput();
        String content = originalMessage.getText();

        if (content == null || !content.contains("<think>")) {
            return clientResponse;
        }

        Matcher matcher = THINK_PATTERN.matcher(content);
        StringBuilder thinkingBuilder = new StringBuilder();
        String cleanedContent = content;

        while (matcher.find()) {
            thinkingBuilder.append(matcher.group(1)).append("\n");
            cleanedContent = cleanedContent.replace(matcher.group(0), "");
        }

        String thinking = thinkingBuilder.toString().trim();
        
        // 1. Safe Metadata handling (ImmutableMap protection)
        Map<String, Object> metadata = new HashMap<>(originalMessage.getMetadata());

        if (!thinking.isEmpty()) {
            metadata.put("think", thinking);
            
            // 2. Safe Context handling
            Map<String, Object> newContext = new HashMap<>(clientResponse.context());
            newContext.put("think", thinking);

            AssistantMessage cleanMessage = new AssistantMessage(cleanedContent.trim());
            cleanMessage.getMetadata().putAll(metadata);
            
            Generation cleanGeneration = new Generation(cleanMessage);

            ChatResponse cleanChatResponse = ChatResponse.builder()
                    .from(chatResponse)
                    .generations(List.of(cleanGeneration))
                    .build();

            return ChatClientResponse.builder()
                    .chatResponse(cleanChatResponse)
                    .context(newContext)
                    .build();
        }

        return clientResponse;
    }

    /**
     * Stateful processor for streaming chunks.
     * It buffers partial tags and maintains "inside/outside" state across tokens.
     */
    private static class StreamingThinkingProcessor {
        private boolean insideThink = false;
        private final StringBuilder thinkingAccumulator = new StringBuilder();
        private String tagBuffer = "";

        public ChatClientResponse process(ChatClientResponse response) {
            if (response == null || response.chatResponse() == null) return response;
            
            ChatResponse chatResponse = response.chatResponse();
            if (chatResponse.getResults() == null || chatResponse.getResults().isEmpty()) return response;

            Generation generation = chatResponse.getResults().get(0);
            AssistantMessage originalMessage = (AssistantMessage) generation.getOutput();
            String chunk = originalMessage.getText();
            if (chunk == null) return response;

            // Combine previous buffer with new chunk
            String fullText = tagBuffer + chunk;
            tagBuffer = "";
            
            StringBuilder outputText = new StringBuilder();
            
            int i = 0;
            while (i < fullText.length()) {
                if (!insideThink) {
                    if (fullText.startsWith("<think>", i)) {
                        insideThink = true;
                        i += 7;
                    } else if (fullText.startsWith("<", i) && i + 7 > fullText.length()) {
                        // Potential partial tag at the end of chunk, buffer it
                        tagBuffer = fullText.substring(i);
                        break;
                    } else {
                        outputText.append(fullText.charAt(i));
                        i++;
                    }
                } else {
                    if (fullText.startsWith("</think>", i)) {
                        insideThink = false;
                        i += 8;
                    } else if (fullText.startsWith("<", i) && i + 8 > fullText.length()) {
                        // Potential partial tag at the end of chunk, buffer it
                        tagBuffer = fullText.substring(i);
                        break;
                    } else {
                        thinkingAccumulator.append(fullText.charAt(i));
                        i++;
                    }
                }
            }

            // Update response with metadata
            Map<String, Object> metadata = new HashMap<>(originalMessage.getMetadata());
            String currentThinking = thinkingAccumulator.toString().trim();
            if (!currentThinking.isEmpty()) {
                metadata.put("think", currentThinking);
            }
            
            AssistantMessage newMessage = new AssistantMessage(outputText.toString());
            newMessage.getMetadata().putAll(metadata);
            
            Generation newGeneration = new Generation(newMessage);
            
            ChatResponse newChatResponse = ChatResponse.builder()
                    .from(chatResponse)
                    .generations(List.of(newGeneration))
                    .build();
            
            Map<String, Object> newContext = new HashMap<>(response.context());
            if (!currentThinking.isEmpty()) {
                newContext.put("think", currentThinking);
            }

            return ChatClientResponse.builder()
                    .chatResponse(newChatResponse)
                    .context(newContext)
                    .build();
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return "ThinkingAdvisor";
    }
}