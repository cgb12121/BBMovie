package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._service._ChatMessageService;
import com.bbmovie.ai_assistant_service.core.low_level._service._ToolExecutionService;
import com.bbmovie.ai_assistant_service.core.low_level._tool._ToolRegistry;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

@RequiredArgsConstructor
public class _ToolExecutingHandlerFactory implements _ChatResponseHandlerFactory {

    private final _ToolRegistry toolRegistry;
    private final _ChatMessageService chatMessageService;
    private final _ToolExecutionService toolExecutionService;
    private final StreamingChatModel chatModel;
    private final SystemMessage systemPrompt;

    @Override
    public StreamingChatResponseHandler create(String sessionId, ChatMemory memory, FluxSink<String> sink, MonoSink<Void> monoSink) {
        return new _ToolExecutingResponseHandler(
                sessionId,
                memory,
                sink,
                monoSink,
                chatModel,
                systemPrompt,
                toolRegistry,
                chatMessageService,
                toolExecutionService
        );
    }
}
