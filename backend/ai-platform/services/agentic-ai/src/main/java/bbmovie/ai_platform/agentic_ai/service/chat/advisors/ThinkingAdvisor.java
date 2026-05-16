package bbmovie.ai_platform.agentic_ai.service.chat.advisors;

import bbmovie.ai_platform.agentic_ai.config.ThinkingAdvisorProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * ThinkingAdvisor intercepts the AI's response to extract and handle "thinking" blocks.
 *
 * <p>Many modern models (like DeepSeek-R1, Qwen-Reasoning, some Claude variants) output their
 * internal thought process wrapped in model-specific tags. This advisor:
 * <ol>
 *   <li>Locates thinking blocks using a stateful parser (streaming) or Regex (unary calls).</li>
 *   <li>Extracts the reasoning content.</li>
 *   <li>Moves the reasoning into the message metadata under the key {@code "think"}.</li>
 *   <li>Removes the tags from the final user-facing content.</li>
 * </ol>
 *
 * <p>Supported tags are fully configurable via {@link ThinkingAdvisorProperties} and
 * {@code ai.advisor.thinking.*} in {@code application.yaml}.
 * By default, both {@code <think>} and {@code <thinking>} variants are supported.
 *
 * <p><b>Design note on the streaming parser:</b><br>
 * The parser maintains per-subscription state so each concurrent stream has its own
 * isolated instance. The partial-tag buffer handles the case where a tag is split across
 * two LLM output chunks (e.g., {@code "<thi"} arrives in one token, {@code "nk>"} in the next).
 * The buffer threshold is derived from the longest configured tag, not hardcoded.
 */
@Component
@RequiredArgsConstructor
public class ThinkingAdvisor implements CallAdvisor, StreamAdvisor {

    private final ThinkingAdvisorProperties properties;

    /**
     * Compiled regex that matches all configured open/close tag pairs.
     * Example result pattern: {@code (?:<think>|<thinking>)(.*?)(?:</think>|</thinking>)}
     * Built once at startup from the configured tag list.
     */
    private Pattern thinkPattern;

    /**
     * The maximum character length across all configured open and close tags.
     * Used by the streaming parser to determine when a potential partial tag
     * must be buffered rather than passed through.
     */
    private int maxTagLength;

    @PostConstruct
    void init() {
        List<String> opens  = properties.getOpenTags();
        List<String> closes = properties.getCloseTags();

        // Build alternating non-capturing groups:
        // (?:<think>|<thinking>) (.*?) (?:</think>|</thinking>)
        String openAlts  = opens.stream().map(Pattern::quote).collect(Collectors.joining("|", "(?:", ")"));
        String closeAlts = closes.stream().map(Pattern::quote).collect(Collectors.joining("|", "(?:", ")"));
        thinkPattern = Pattern.compile(openAlts + "(.*?)" + closeAlts, Pattern.DOTALL);

        maxTagLength = IntStream.concat(
                opens.stream().mapToInt(String::length),
                closes.stream().mapToInt(String::length)
        ).max().orElse(16);
    }

    // ─── Unary (non-streaming) ────────────────────────────────────────────────
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        return processResponse(chain.nextCall(request));
    }

    private ChatClientResponse processResponse(ChatClientResponse clientResponse) {
        if (clientResponse == null || clientResponse.chatResponse() == null) return clientResponse;

        ChatResponse chatResponse = clientResponse.chatResponse();
        List<Generation> generations = chatResponse.getResults();
        if (generations == null || generations.isEmpty()) return clientResponse;

        Generation generation = generations.get(0);
        AssistantMessage originalMessage = (AssistantMessage) generation.getOutput();
        String content = originalMessage.getText();
        if (content == null) return clientResponse;

        // Pre-check: skip Regex entirely if no open tag is present in the full response.
        boolean hasAnyTag = properties.getOpenTags().stream().anyMatch(content::contains);
        if (!hasAnyTag) return clientResponse;

        Matcher matcher = thinkPattern.matcher(content);
        StringBuilder thinkingBuilder = new StringBuilder();
        String cleanedContent = content;

        while (matcher.find()) {
            thinkingBuilder.append(matcher.group(1)).append("\n");
            cleanedContent = cleanedContent.replace(matcher.group(0), "");
        }

        String thinking = thinkingBuilder.toString().trim();
        if (thinking.isEmpty()) return clientResponse;

        // Build new message with thinking in metadata, clean text in content.
        Map<String, Object> metadata = new HashMap<>(originalMessage.getMetadata());
        metadata.put("think", thinking);

        Map<String, Object> newContext = new HashMap<>(clientResponse.context());
        newContext.put("think", thinking);

        AssistantMessage cleanMessage = new AssistantMessage(cleanedContent.trim());
        cleanMessage.getMetadata().putAll(metadata);

        ChatResponse cleanChatResponse = ChatResponse.builder()
                .from(chatResponse)
                .generations(List.of(new Generation(cleanMessage)))
                .build();

        return ChatClientResponse.builder()
                .chatResponse(cleanChatResponse)
                .context(newContext)
                .build();
    }

    // ─── Streaming ────────────────────────────────────────────────────────────

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        return Flux.defer(() -> {
            // Flux.defer ensures each subscriber gets its own isolated processor instance,
            // which is critical for concurrent streaming requests.
            StreamingThinkingProcessor processor = new StreamingThinkingProcessor(
                    properties.getOpenTags(),
                    properties.getCloseTags(),
                    maxTagLength
            );
            return chain.nextStream(request).map(processor::process);
        });
    }

    // ─── Inner class: stateful streaming parser ───────────────────────────────

    /**
     * Stateful parser for streaming chunks.
     *
     * <p>Maintains inside/outside-thinking state across tokens and buffers potential
     * partial tags that span chunk boundaries. Supports multiple configurable tag pairs.
     *
     * <p><b>Partial tag buffering:</b><br>
     * When {@code '<'} is encountered with fewer remaining characters than the longest
     * configured tag, the remaining text is buffered and prepended to the next chunk.
     * This handles split tags like {@code "...<thi"} + {@code "nk>..."} across two tokens.
     *
     * <p><b>Paired tag matching:</b><br>
     * When openTags[i] is matched, the processor locks onto closeTags[i] as the expected
     * close tag. This prevents cross-pair matching (e.g., {@code <think>} closed by {@code </thinking>}).
     */
    private static class StreamingThinkingProcessor {

        private final List<String> openTags;
        private final List<String> closeTags;

        /**
         * Maximum character length across all configured tags.
         * The partial-tag buffer is triggered when fewer characters remain in the
         * current chunk than this value, conservatively preventing missed tags.
         */
        private final int maxTagLength;

        private boolean insideThink = false;

        /**
         * The specific close tag to look for, determined when an open tag is matched.
         * Uses index-pairing: openTags[i] → closeTags[i].
         */
        private String activeCloseTag = null;

        private final StringBuilder thinkingAccumulator = new StringBuilder();

        /** Leftover text from the previous chunk that starts with {@code '<'} */
        private String tagBuffer = "";

        StreamingThinkingProcessor(List<String> openTags, List<String> closeTags, int maxTagLength) {
            this.openTags     = openTags;
            this.closeTags    = closeTags;
            this.maxTagLength = maxTagLength;
        }

        ChatClientResponse process(ChatClientResponse response) {
            if (response == null || response.chatResponse() == null) return response;

            ChatResponse chatResponse = response.chatResponse();
            if (chatResponse.getResults() == null || chatResponse.getResults().isEmpty()) return response;

            Generation generation = chatResponse.getResults().get(0);
            AssistantMessage originalMessage = (AssistantMessage) generation.getOutput();
            String chunk = originalMessage.getText();
            if (chunk == null) return response;

            String fullText = tagBuffer + chunk;
            tagBuffer = "";

            StringBuilder outputText = new StringBuilder();
            int i = 0;

            while (i < fullText.length()) {
                char c = fullText.charAt(i);

                if (!insideThink) {
                    String matchedOpen = matchTagAt(fullText, i, openTags);
                    if (matchedOpen != null) {
                        // Entered a thinking block — lock in the paired close tag.
                        insideThink = true;
                        int idx = openTags.indexOf(matchedOpen);
                        activeCloseTag = (idx < closeTags.size()) ? closeTags.get(idx) : closeTags.get(0);
                        i += matchedOpen.length();
                    } else if (c == '<' && (fullText.length() - i) < maxTagLength) {
                        // Potential partial open tag at chunk boundary — buffer it.
                        tagBuffer = fullText.substring(i);
                        break;
                    } else {
                        outputText.append(c);
                        i++;
                    }
                } else {
                    if (activeCloseTag != null && fullText.startsWith(activeCloseTag, i)) {
                        // Exited the thinking block.
                        insideThink = false;
                        i += activeCloseTag.length();
                        activeCloseTag = null;
                    } else if (c == '<' && (fullText.length() - i) < maxTagLength) {
                        // Potential partial close tag at chunk boundary — buffer it.
                        tagBuffer = fullText.substring(i);
                        break;
                    } else {
                        thinkingAccumulator.append(c);
                        i++;
                    }
                }
            }

            // Build the updated response chunk with clean content and thinking in metadata.
            Map<String, Object> metadata = new HashMap<>(originalMessage.getMetadata());
            String currentThinking = thinkingAccumulator.toString().trim();
            if (!currentThinking.isEmpty()) {
                metadata.put("think", currentThinking);
            }

            AssistantMessage newMessage = new AssistantMessage(outputText.toString());
            newMessage.getMetadata().putAll(metadata);

            ChatResponse newChatResponse = ChatResponse.builder()
                    .from(chatResponse)
                    .generations(List.of(new Generation(newMessage)))
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

        /**
         * Returns the first tag from {@code tags} that begins exactly at position {@code pos}
         * in {@code text}, or {@code null} if no tag matches at that position.
         */
        private String matchTagAt(String text, int pos, List<String> tags) {
            for (String tag : tags) {
                if (text.startsWith(tag, pos)) {
                    return tag;
                }
            }
            return null;
        }
    }

    @Override
    public int getOrder() { return 0; }

    @Override
    public String getName() { return "ThinkingAdvisor"; }
}