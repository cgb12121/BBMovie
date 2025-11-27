package com.bbmovie.ai_assistant_service.config.ai.logging.level;

import com.bbmovie.ai_assistant_service.config.ai.logging.Logging;
import com.bbmovie.ai_assistant_service.utils.log.AnsiRainbowUtil;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.List;
import java.util.stream.Collectors;

import static org.fusesource.jansi.Ansi.ansi;

public class VerboseLogging implements Logging {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(VerboseLogging.class);

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        ModelProvider provider = requestContext.modelProvider();
        ChatRequest request = requestContext.chatRequest();
        List<ChatMessage> messages = request.messages();
        List<ToolSpecification> tools = request.toolSpecifications();

        StringBuilder sb = new StringBuilder("\n");

        sb.append(ansi().fgBrightCyan().a("[listener] Request to ")
                .a(provider).reset().a("\n"));

        String messagesString = messages.stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
        sb.append(AnsiRainbowUtil.getLightRainbow(messagesString));
        sb.append("\n");

        if (tools != null && !tools.isEmpty()) {
            String toolsString = tools.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"));

            sb.append(ansi().fgBrightCyan().a("Tools:").reset().a("\n"));
            sb.append(AnsiRainbowUtil.getLightRainbow(toolsString));
            sb.append("\n");
        }

        log.debug(sb.toString());
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        ModelProvider provider = responseContext.modelProvider();
        ChatResponse response = responseContext.chatResponse();

        StringBuilder sb = new StringBuilder("\n");

        sb.append(ansi().fgBrightGreen().a("[listener] Response from ")
                .a(provider).reset().a("\n"));

        String aiMessage = response.aiMessage() != null ?
                response.aiMessage().toString() : "[No AI Message]";
        sb.append(AnsiRainbowUtil.getLightRainbow(aiMessage));
        sb.append("\n");

        log.debug(sb.toString());
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        ModelProvider provider = errorContext.modelProvider();
        Throwable error = errorContext.error();

        String sb = "\n" + ansi().fgRed().a("[listener] Error from ")
                .a(provider).reset().a("\n") +
                ansi().fgRed().a(error.getMessage()).reset() +
                "\n";

        log.error(sb, error);
    }
}
