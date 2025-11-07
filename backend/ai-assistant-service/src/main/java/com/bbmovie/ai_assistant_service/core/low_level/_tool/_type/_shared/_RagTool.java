package com.bbmovie.ai_assistant_service.core.low_level._tool._type._shared;

import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._dto._RagRetrievalResult;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._InteractionType;
import com.bbmovie.ai_assistant_service.core.low_level._service._AuditService;
import com.bbmovie.ai_assistant_service.core.low_level._service._RagService;
import com.bbmovie.ai_assistant_service.core.low_level._tool._AiTools;
import com.bbmovie.ai_assistant_service.core.low_level._utils._MetricsUtil;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component("_RagTool")
@Qualifier("_PublicTools")
@SuppressWarnings("unused")
public class _RagTool implements _AiTools {

    private final _RagService ragService;
    private final _AuditService auditService;

    @Autowired
    public _RagTool(_RagService ragService, _AuditService auditService) {
        this.ragService = ragService;
        this.auditService = auditService;
    }

    @Tool(
            name = "retrieve_rag_movies",
            value = """
                    Use this tool to retrieve semantically relevant movie documents based on user queries.
                    Ideal for movie recommendations, scientific movies, romance, genres, or contextual enrichment.
                    Inputs:
                        - sessionId (UUID): Chat session identifier
                        - queryMovie (String): The user's movie-related query
                        - topK (int): Number of relevant documents to retrieve (max 6 recommended)
                    Output:
                        - A structured _RagRetrievalResult containing ranked movie metadata and descriptions.
                    """
    )
    public _RagRetrievalResult retrieveRagMovies(UUID sessionId, String queryMovie, int topK) {
        long start = System.currentTimeMillis();
        log.info("[tool][RAG] Invoked retrieveRagMovies(sessionId={}, queryMovie='{}', topK={})",
                sessionId, queryMovie, topK);

        try {
            _RagRetrievalResult result = ragService.retrieveMovieContext(sessionId, queryMovie, topK)
                    .subscribeOn(Schedulers.boundedElastic())
                    .blockOptional(Duration.ofSeconds(10))
                    .orElseGet(() -> {
                        log.warn("[tool][RAG] Retrieval returned empty result.");
                        return new _RagRetrievalResult("", List.of());
                    });

            long latency = System.currentTimeMillis() - start;
            int resultCount = result.documents() != null ? result.documents().size() : 0;

            _Metrics metrics = _MetricsUtil.get(latency, null, "rag-tool", "retrieve_rag_movies");

            // Audit successful retrieval
            auditService.recordInteraction(
                            sessionId,
                            _InteractionType.TOOL_EXECUTION_RESULT,
                            Map.of(
                                    "tool", "retrieve_rag_movies",
                                    "query", queryMovie,
                                    "topK", topK,
                                    "results", resultCount
                            ),
                            metrics
                    ).doOnSuccess(v -> log.debug("[audit][RAG] Tool audit recorded successfully."))
                    .doOnError(e -> log.warn("[audit][RAG] Failed to record tool audit: {}", e.getMessage()))
                    .subscribe();

            return result;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;

            _Metrics metrics = _MetricsUtil.get(latency, null, "rag-tool", "retrieve_rag_movies");
            log.error("[tool][RAG] Error executing tool: {}", e.getMessage(), e);

            // Audit failure
            auditService.recordInteraction(
                            sessionId,
                            _InteractionType.TOOL_EXECUTION_RESULT,
                            Map.of(
                                    "tool", "retrieve_rag_movies",
                                    "query", queryMovie,
                                    "error", e.getMessage()
                            ),
                            metrics
                    ).doOnError(ex -> log.warn("[audit][RAG] Failed to record error audit: {}", ex.getMessage()))
                    .subscribe();

            return new _RagRetrievalResult("", List.of());
        }
    }
}
