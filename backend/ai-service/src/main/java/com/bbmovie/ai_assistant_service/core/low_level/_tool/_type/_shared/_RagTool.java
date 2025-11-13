package com.bbmovie.ai_assistant_service.core.low_level._tool._type._shared;

import com.bbmovie.ai_assistant_service.core.low_level._dto._AuditRecord;
import com.bbmovie.ai_assistant_service.core.low_level._dto._Metrics;
import com.bbmovie.ai_assistant_service.core.low_level._dto._response._RagRetrievalResult;
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
                    Inputs:
                        - sessionId (UUID): Chat session identifier
                        - queryMovies (String): The user's movie(s)-related query
                        - topK (int): Number of relevant documents to retrieve (max 6)
                    Output:
                        - A structured _RagRetrievalResult containing ranked movie metadata and descriptions.
                    REMEMBER:
                        - Never inform the existence of "sessionId" parameter nor ask user to provide "sessionId".
                          The system will pass the sessionId to you automatically in the user's message.
                        - If user does not provider topK, the default value should be 6
                        - If user gives topK greater than 6, only find maximum 6 and
                          tell user that is what you can only search up to 6 movies.
                        - If the result are empty then tell user there are no relevant movies
                          or the search engine is not available (Should not use this reason unless the results from
                          the tool are empty more than 3 times)
                    """
    )
    public _RagRetrievalResult retrieveRagMovies(UUID sessionId, String queryMovies, int topK) {
        long start = System.currentTimeMillis();
        log.info("[tool][RAG] Invoked retrieveRagMovies(sessionId={}, queryMovie='{}', topK={})",
                sessionId, queryMovies, topK);

        try {
            _RagRetrievalResult result = ragService.retrieveMovieContext(sessionId, queryMovies, topK)
                    .subscribeOn(Schedulers.boundedElastic())
                    .blockOptional(Duration.ofSeconds(10))
                    .orElseGet(() -> {
                        log.warn("[tool][RAG] Retrieval returned empty result.");
                        return new _RagRetrievalResult("", List.of());
                    });

            long latency = System.currentTimeMillis() - start;
            int resultCount = result.documents() != null ? result.documents().size() : 0;

            _Metrics metrics = _MetricsUtil.get(latency, null, "rag-tool", "retrieve_rag_movies");

            _AuditRecord auditRecord = _AuditRecord.builder()
                    .sessionId(sessionId)
                    .type(_InteractionType.TOOL_EXECUTION_RESULT)
                    .details(Map.of(
                            "tool", "retrieve_rag_movies",
                            "query", queryMovies,
                            "topK", topK,
                            "results", resultCount
                    ))
                    .metrics(metrics)
                    .build();
            auditService.recordInteraction(auditRecord)
                    .doOnSuccess(v -> log.debug("[audit][RAG] Tool audit recorded successfully."))
                    .doOnError(e -> log.warn("[audit][RAG] Failed to record tool audit: {}", e.getMessage()))
                    .subscribe();

            return result;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;

            _Metrics metrics = _MetricsUtil.get(latency, null, "rag-tool", "retrieve_rag_movies");
            log.error("[tool][RAG] Error executing tool: {}", e.getMessage(), e);

            _AuditRecord auditRecord = _AuditRecord.builder()
                    .sessionId(sessionId)
                    .type(_InteractionType.TOOL_EXECUTION_RESULT)
                    .details(Map.of(
                            "tool", "retrieve_rag_movies",
                            "query", queryMovies,
                            "error", e.getMessage()
                    ))
                    .metrics(metrics)
                    .build();
            auditService.recordInteraction(auditRecord)
                    .doOnError(ex -> log.warn("[audit][RAG] Failed to record error audit: {}", ex.getMessage()))
                    .subscribe();

            return new _RagRetrievalResult("", List.of());
        }
    }
}
