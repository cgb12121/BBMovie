package com.bbmovie.ai_assistant_service.core.low_level._config;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnBooleanProperty(name = "ai.experimental.enabled")
public class _ChatListener implements ChatModelListener {

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        ModelProvider provider = requestContext.modelProvider();
        ChatRequest request = requestContext.chatRequest();
        List<ChatMessage> messages = request.messages();
        List<ToolSpecification> tools = request.toolSpecifications();
        log.info("[listener] Request to {}: \n{} \n tools[{}] ", provider, messages, tools);
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        ModelProvider provider = responseContext.modelProvider();
        ChatResponse response = responseContext.chatResponse();
        log.debug("[listener] Response from {}: \n{}", provider, response.aiMessage());
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        ModelProvider provider = errorContext.modelProvider();
        Throwable error = errorContext.error();
        log.error("[listener] Error from {}: \n{}", provider, error.getMessage(), error);
    }
}
