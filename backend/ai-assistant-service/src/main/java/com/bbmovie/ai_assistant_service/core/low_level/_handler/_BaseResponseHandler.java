package com.bbmovie.ai_assistant_service.core.low_level._handler;

import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

@Slf4j
public abstract class _BaseResponseHandler implements StreamingChatResponseHandler {

    protected final FluxSink<String> sink;
    protected final MonoSink<Void> monoSink;

    protected _BaseResponseHandler(FluxSink<String> sink, MonoSink<Void> monoSink) {
        this.sink = sink;
        this.monoSink = monoSink;
    }

    @Override
    public void onPartialResponse(String partialResponse) {
        sink.next(partialResponse);
    }

    @Override
    public void onError(Throwable error) {
        log.error("[streaming] Error during streaming", error);
        monoSink.error(error);
    }
}
