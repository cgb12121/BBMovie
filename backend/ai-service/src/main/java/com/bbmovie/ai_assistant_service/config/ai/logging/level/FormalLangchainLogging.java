package com.bbmovie.ai_assistant_service.config.ai.logging.level;

import com.bbmovie.ai_assistant_service.config.ai.logging.LangchainLogging;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lightweight, safe chat listener that logs a short, readable summary of requests/responses.
 * Defensive about ChatMessage implementations: it checks known classes, then falls back to reflection,
 * then finally to toString() when nothing else works.
 */
public class FormalLangchainLogging implements LangchainLogging {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(FormalLangchainLogging.class);

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        ModelProvider provider = requestContext.modelProvider();
        ChatRequest request = requestContext.chatRequest();
        List<ChatMessage> messages = request.messages();
        List<ToolSpecification> tools = request.toolSpecifications();

        String header = String.format("[listener] Request to %s", provider.name());

        int msgCount = messages != null ? messages.size() : 0;
        int toolCount = tools != null ? tools.size() : 0;

        StringBuilder sb = new StringBuilder("\n")
                .append(header)
                .append("\n")
                .append("Messages: ").append(msgCount)
                .append("\n")
                .append("Tools: ").append(toolCount)
                .append("\n");

        if (messages != null && !messages.isEmpty()) {
            // Show a preview of last up to 5 messages with type
            int start = Math.max(0, messages.size() - 5);
            List<ChatMessage> tail = messages.subList(start, messages.size());
            for (ChatMessage m : tail) {
                String role = safeMessageTypeName(m);
                String preview = safeExtractMessageText(m);
                sb.append(role).append(":")
                        .append("\n")
                        .append(preview)
                        .append("\n");
            }
        }

        if (tools != null && !tools.isEmpty()) {
            sb.append("\n")
                    .append("Tool specs (names only):")
                    .append("\n");
            String toolNames = tools.stream()
                    .map(ToolSpecification::name)
                    .limit(10)
                    .collect(Collectors.joining(", "));
            sb.append(toolNames);
            if (tools.size() > 10) {
                sb.append("\n").append("... and ").append(tools.size() - 10).append(" more");
            }
            sb.append("\n");
        }

        log.debug(sb.toString());
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        ModelProvider provider = responseContext.modelProvider();
        ChatResponse response = responseContext.chatResponse();

        String header = String.format("[listener] Response from %s", provider.name());

        StringBuilder sb = new StringBuilder("\n").append(header).append("\n");

        AiMessage ai = response.aiMessage();
        if (ai == null) {
            sb.append("AI Message: <unavailable>\n");
        } else {
            sb.append("AI Message:\n")
                    .append(ai.text() != null ? ai.text() : "<no text>")
                    .append("\n");

            if (ai.thinking() != null && !ai.thinking().isBlank()) {
                sb.append("AI Thinking:\n")
                        .append(ai.thinking())
                        .append("\n");
            }

            if (ai.toolExecutionRequests() != null && !ai.toolExecutionRequests().isEmpty()) {
                sb.append("Tool Requests:")
                        .append("\n");

                StringBuilder toolRq = new StringBuilder();
                ai.toolExecutionRequests().forEach(req -> toolRq.append("- ")
                        .append(req.name())
                        .append(" => args=")
                        .append(req.arguments())
                );
                sb.append(toolRq.toString()).append("\n");
            }
        }

        log.debug(sb.toString());
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        ModelProvider provider = errorContext.modelProvider();
        Throwable error = errorContext.error();

        String header = String.format("[listener] Error from %s", provider.name());

        String errorMsg = error != null && error.getMessage() != null
                ? error.getMessage()
                : error != null ? error.getClass().getSimpleName() : "<unknown>";

        String sb = "\n" +
                header +
                "\n" +
                "Message: " + errorMsg +
                "\n";

        log.error(sb, error);
    }

    /**
     * Safe extraction of a short, human-readable preview for an arbitrary ChatMessage.
     * Tries the common concrete types first (UserMessage, SystemMessage, AssistantMessage),
     * then falls back to reflection (text/content/getContent) and finally to toString().
     */
    private String safeExtractMessageText(ChatMessage m) {
        if (m == null) return "<null>";

        // Known LangChain4j message types — try direct casts first
        try {
            switch (m) {
                case UserMessage userMessage -> {
                    return userMessage.singleText();
                }
                case SystemMessage systemMessage -> {
                    return systemMessage.text();
                }
                case AiMessage aiMessage -> {
                    return aiMessage.text();
                }
                case ToolExecutionResultMessage toolExecutionResultMessage -> {
                    return toolExecutionResultMessage.text();
                }
                default -> log.info("Unknown message type: {}", m.getClass().getName());
            }
        } catch (Throwable ignored) {
            // If cast fails unexpectedly, continue to reflective fallback
        }

        // Fallback: try common method names via reflection
        String[] candidateMethods = new String[]{"text", "content", "getContent", "getText", "getMessage"};
        for (String methodName : candidateMethods) {
            try {
                Method method = m.getClass().getMethod(methodName);
                Object val = method.invoke(m);
                if (val != null) return String.valueOf(val);
            } catch (NoSuchMethodException ignored) {
                // ignore — try next
            } catch (Throwable t) {
                // if reflection invocation throws, break and fallback to toString
                break;
            }
        }

        // Last salvage: use toString (truncated)
        return m.toString();
    }

    /**
     * Return a readable name for a message type. Uses ChatMessage.type() where available.
     * If not present, falls back to class simple name.
     */
    private String safeMessageTypeName(ChatMessage m) {
        if (m == null) return "<null>";
        try {
            ChatMessageType t = m.type();
            if (t != null) return t.name();
        } catch (Throwable ignored) {

        }
        return m.getClass().getSimpleName();
    }

}