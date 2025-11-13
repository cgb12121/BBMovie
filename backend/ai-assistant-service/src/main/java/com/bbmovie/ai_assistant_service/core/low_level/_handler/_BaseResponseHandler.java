package com.bbmovie.ai_assistant_service.core.low_level._handler;

import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._utils._ThinkingSanitizer;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
import dev.langchain4j.model.chat.response.*;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.MonoSink;

/**
 * Base implementation of a reactive {@link StreamingChatResponseHandler} used to handle
 * streaming responses from LangChain4j (e.g., Ollama, OpenAI, or local models) in a non-blocking way.
 *
 * <p>
 * This class acts as a bridge between the low-level LangChain4j callback-based streaming model
 * and the reactive programming paradigm used throughout the application (Reactor / WebFlux).
 * </p>
 *
 * <h2>Conceptual Overview</h2>
 *
 * <p>
 * LangChain4j delivers AI responses asynchronously through callbacks like
 * {@link #onPartialResponse(String)} and {@link #onCompleteResponse(ChatResponse)}}.
 * However, in our reactive Spring Boot architecture, we expose AI responses as
 * {@code Flux<_ChatStreamChunk>} streams back to the REST controller and ultimately the client.
 * </p>
 *
 * <p>
 * To connect these two models, we use Reactor’s {@link FluxSink} and {@link MonoSink}:
 * </p>
 *
 * <ul>
 *   <li><b>{@link FluxSink}&lt;String&gt; sink</b> — Represents the ongoing stream of AI tokens or partial
 *   responses being emitted. Each token (or message fragment) is pushed into the {@code sink}
 *   via {@link #onPartialResponse(String)}. This enables real-time streaming to the client (e.g., via SSE or WebFlux).</li>
 *
 *   <li><b>{@link MonoSink}&lt;Void&gt; monoSink</b> — Represents the terminal signal of the chat session.
 *   It completes successfully when the model finishes streaming (or tool calls are done)
 *   and emits an error if something unrecoverable happens (e.g., model failure or timeout).</li>
 * </ul>
 *
 * <h2>Why Both?</h2>
 *
 * <p>
 * The {@code FluxSink} and {@code MonoSink} together represent two distinct lifecycles:
 * </p>
 *
 * <ul>
 *   <li><b>FluxSink</b> → Handles the <em>ongoing data stream</em> (token-by-token emission).</li>
 *   <li><b>MonoSink</b> → Signals the <em>completion of the entire streaming session</em>.</li>
 * </ul>
 *
 * <p>
 * This separation allows us to:
 * </p>
 * <ul>
 *   <li>Stream model output in real-time as tokens are generated.</li>
 *   <li>Wait reactively for the model or tool execution pipeline to finish before auditing, saving messages, etc.</li>
 * </ul>
 *
 * <h2>Subclass Responsibilities</h2>
 *
 * <p>
 * Concrete subclasses such as {@code _ToolExecutingResponseHandler} or {@code _SimpleStreamingResponseHandler}
 * should extend this class and override lifecycle methods like:
 * </p>
 *
 * <ul>
 *   <li>{@link #onPartialResponse(String)} — to intercept tokens and emit them as structured {@code _ChatStreamChunk}s.</li>
 *   <li>{@link #onCompleteResponse(ChatResponse)} — to handle full AI messages,
 *   save chat history, or perform tool executions.</li>
 *   <li>{@link #onError(Throwable)} — to report failures gracefully, record audits, and complete the sinks safely.</li>
 * </ul>
 *
 * <h2>Reactive Contract</h2>
 *
 * <ul>
 *   <li>Every successful stream <b>must</b> call {@code monoSink.success()} when finished.</li>
 *   <li>All emitted data should flow through {@code sink.next(...)}.</li>
 *   <li>On error, call {@code monoSink.error(...)} and optionally emit a system fallback message via {@code sink.next(...)}.</li>
 *   <li>Do not block inside handler methods — use non-blocking operators or {@code subscribeOn(Schedulers.boundedElastic())} where needed.</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 *
 * <pre>{@code
 * StreamingChatResponseHandler handler = new _ToolExecutingResponseHandler(
 *     sessionId, chatMemory, sink, monoSink, modelFactory, systemPrompt, toolRegistry,
 *     messageService, toolExecutionService, auditService, System.currentTimeMillis()
 * );
 *
 * modelFactory.getModel(aiMode).chat(chatRequest, handler);
 * }</pre>
 *
 * @author Me
 */
public abstract class _BaseResponseHandler implements StreamingChatResponseHandler {

    private static final _Logger log = _LoggerFactory.getLogger(_BaseResponseHandler.class);

    /**
     * Reactive stream emitter for partial model responses (tokens, chunks, etc.).
     * Used to push incremental outputs downstream in a non-blocking way.
     */
    protected final FluxSink<_ChatStreamChunk> sink;

    /**
     * Completion signal for the entire AI session.
     * Used to mark success or failure of the overall chat processing pipeline.
     */
    protected final MonoSink<Void> monoSink;

    /**
     * Constructs the handler with the reactive emitters used to bridge LangChain4j streaming
     * into a Project Reactor-based pipeline.
     *
     * @param sink     the sink for partial streaming data
     * @param monoSink the sink signaling session completion or failure
     */
    protected _BaseResponseHandler(FluxSink<_ChatStreamChunk> sink, MonoSink<Void> monoSink) {
        this.sink = sink;
        this.monoSink = monoSink;
    }

    /**
     * Called when the model has finished generating the complete response.
     * The default implementation logs thinking for audit purposes (if available),
     * then completes both the {@link FluxSink} and {@link MonoSink}.
     * <p>
     * <b>Note:</b> Thinking is used for audit/logging only, not sent to clients by default.
     * Content is streamed directly as it arrives via {@link #onPartialResponse(String)}.
     * <p>
     * <b>Important:</b> Subclasses that override this method should call
     * {@link #handleThinking(ChatResponse)} to ensure thinking is logged/emitted properly.
     *
     * @param chatResponse the full AI-generated chat response
     */
    @Override
    public void onCompleteResponse(ChatResponse chatResponse) {
        // Handle thinking (log for audit, optionally emit to clients)
        handleThinking(chatResponse);
        
        // Default implementation: complete the stream
        sink.complete();
        monoSink.success();
    }

    /**
     * Handles thinking content: logs it for audit purposes only (never sent to clients).
     * This method can be called by subclasses to ensure thinking is properly handled.
     *
     * @param chatResponse the chat response containing thinking
     */
    protected void handleThinking(ChatResponse chatResponse) {
        String thinking = chatResponse.aiMessage().thinking();
        if (thinking == null || thinking.isBlank()) {
            return;
        }

        // Log thinking for audit purposes only (sanitized, never sent to clients)
        String sanitizedThinking = _ThinkingSanitizer.sanitize(thinking);
        if (sanitizedThinking != null && !sanitizedThinking.isBlank()) {
            log.trace("[thinking][audit] AI thinking trace: {}", sanitizedThinking);
        }
    }

    /**
     * Called each time the model emits a partial token or content fragment.
     * The default implementation streams content directly to clients in real-time.
     * 
     * <p>Note: Thinking is handled separately in {@link #onCompleteResponse(ChatResponse)}
     * for audit/logging purposes only.
     *
     * @param partialResponse a fragment of the AI's streamed output
     */
    @Override
    public void onPartialResponse(String partialResponse) {
        // Stream content directly as it arrives (no buffering needed)
        sink.next(_ChatStreamChunk.assistant(partialResponse));
    }

    /**
     * Called when an unrecoverable error occurs during streaming.
     * Logs the error and signals the {@link MonoSink} with failure.
     * Note: Error messages are handled by the Flux chain's onErrorResume,
     * so we don't emit to sink here to avoid duplicate messages.
     *
     * @param error the throwable that caused the failure
     */
    @Override
    public void onError(Throwable error) {
        log.error("[streaming] Error during streaming: {}", error.getMessage(), error);
        // Don't emit to sink here - let the Flux chain's onErrorResume handle it
        // to avoid duplicate error messages
        try {
            if (!sink.isCancelled()) {
                sink.complete();
            }
        } catch (Exception e) {
            // Sink might already be completed/errored, ignore
            log.trace("[streaming] Could not complete sink on error: {}", e.getMessage());
        }
        monoSink.error(error);
    }
}

