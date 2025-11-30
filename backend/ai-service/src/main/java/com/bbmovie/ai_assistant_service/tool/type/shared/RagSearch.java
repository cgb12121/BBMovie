package com.bbmovie.ai_assistant_service.tool.type.shared;

import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import com.bbmovie.ai_assistant_service.dto.Metrics;
import com.bbmovie.ai_assistant_service.dto.response.RagRetrievalResult;
import com.bbmovie.ai_assistant_service.entity.model.InteractionType;
import com.bbmovie.ai_assistant_service.service.AuditService;
import com.bbmovie.ai_assistant_service.service.RagService;
import com.bbmovie.ai_assistant_service.utils.MetricsUtil;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
@Component
@Qualifier("commonTools")
@RequiredArgsConstructor
public class RagSearch implements CommonTools {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(RagSearch.class);

    private final RagService ragService;
    private final AuditService auditService;

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
    public RagRetrievalResult retrieveRagMovies(UUID sessionId, String queryMovies, int topK) {
        long start = System.currentTimeMillis();
        log.info("[tool][RAG] Invoked retrieveRagMovies(sessionId={}, queryMovie='{}', topK={})",
                sessionId, queryMovies, topK);

        try {
            RagRetrievalResult result = ragService.retrieveMovieContext(sessionId, queryMovies, topK)
                    .subscribeOn(Schedulers.boundedElastic())
                    .blockOptional(Duration.ofSeconds(10))
                    .orElseGet(() -> {
                        log.warn("[tool][RAG] Retrieval returned empty result.");
                        return new RagRetrievalResult("", List.of());
                    });

            AuditRecord auditRecord = createAuditRecord(sessionId, queryMovies, topK, start, result, null);
            auditService.recordInteraction(auditRecord)
                    .doOnSuccess(v -> log.debug("[audit][RAG] Tool audit recorded successfully."))
                    .doOnError(e -> log.warn("[audit][RAG] Failed to record tool audit: {}", e.getMessage()))
                    .subscribe();

            return result;
        } catch (Exception e) {
            log.error("[tool][RAG] Error executing tool: {}", e.getMessage(), e);

            AuditRecord auditRecord = createAuditRecord(sessionId, queryMovies, topK, start, new RagRetrievalResult("", List.of()), e);
            auditService.recordInteraction(auditRecord)
                    .doOnError(ex -> log.warn("[audit][RAG] Failed to record error audit: {}", ex.getMessage()))
                    .subscribe();

            return new RagRetrievalResult("", List.of());
        }
    }

    private static AuditRecord createAuditRecord(
            UUID sessionId, String queryMovies, int topK, long start, RagRetrievalResult result, Exception error) {
        long latency = System.currentTimeMillis() - start;
        int resultCount = result.documents() != null
                ? result.documents().size()
                : 0;

        Metrics metrics = MetricsUtil.get(latency, null, "rag-tool", "retrieve_rag_movies");

        Map<String, Object> details = new HashMap<>();
        details.put("tool", "retrieve_rag_movies");
        details.put("query", queryMovies);
        if (error == null) {
            details.put("topK", topK);
            details.put("results", resultCount);
        } else {
            details.put("error", error.getMessage());
        }


        return AuditRecord.builder()
                .sessionId(sessionId)
                .type(InteractionType.TOOL_EXECUTION_RESULT)
                .details(details)
                .metrics(metrics)
                .build();
    }
}
