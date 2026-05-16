package bbmovie.ai_platform.agentic_ai.service.tool.tools;

import bbmovie.ai_platform.agentic_ai.entity.AgentMemory;
import bbmovie.ai_platform.agentic_ai.repository.MemoryRepository;
import bbmovie.ai_platform.agentic_ai.service.personalize.PersonalizationService;
import bbmovie.ai_platform.agentic_ai.utils.AiConstants;
import bbmovie.ai_platform.aop_policy.annotation.Monitored;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Configuration;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MemoryTools {

    private final MemoryRepository memoryRepository;
    private final PersonalizationService personalizationService;

    /**
     * Saves a concise fact about the user into long-term memory.
     *
     * <p><b>Design contract:</b>
     * <ul>
     *   <li>This tool is <b>fire-and-forget</b> — the AI must NOT wait for the result or report
     *       success/failure to the user. The tool returns immediately after triggering the async save.</li>
     *   <li>If the per-user memory cap ({@value AiConstants#MAX_MEMORIES_PER_USER}) is reached,
     *       the save is silently skipped. The user is notified externally (e.g., via the memories dashboard),
     *       never through the AI's chat response.</li>
     *   <li>Facts are written to both PostgreSQL (user-visible, capped) and Qdrant (semantic index, uncapped).</li>
     * </ul>
     */
    @Tool(description = """
            Saves a concise, summarized fact about the user into long-term memory for future personalization.
            IMPORTANT: Summarize the fact to under 100 characters before calling this tool.
            Do NOT report success or failure to the user - this runs silently in the background.
            Call this tool and immediately continue with your response.
            """)
    @Monitored
    public String saveMemory(
            @ToolParam(description = "A concise, summarized fact about the user (max 100 characters). Example: 'Prefers dark mode and concise answers'") String fact,
            ToolContext toolContext
    ) {
        UUID userId = (UUID) toolContext.getContext().get("userId");
        log.info("[MemoryTools] saveMemory triggered for user: {}", userId);

        // Quick limit check — synchronous but fast (indexed count query).
        // If limit is reached: silently return. Do NOT notify the AI.
        try {
            Long count = memoryRepository.countByUserId(userId)
                    .block(Duration.ofSeconds(1));
            if (count != null && count >= AiConstants.MAX_MEMORIES_PER_USER) {
                log.info("[MemoryTools] Memory cap reached ({}/{}) for user: {}. Silently skipping.",
                        count, AiConstants.MAX_MEMORIES_PER_USER, userId);
                return ""; // Silent — AI continues without knowing about the limit
            }
        } catch (Exception e) {
            // If count check fails, attempt to save anyway (fail-open)
            log.warn("[MemoryTools] Count check failed for user {}, proceeding with save: {}", userId, e.getMessage());
        }

        // Fire-and-forget: build the entity and kick off async save.
        // The AI returns its response immediately; persistence happens in the background.
        AgentMemory memory = AgentMemory.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fact(fact)
                .build();

        memoryRepository.save(memory)
                .then(personalizationService.recordBehavior(userId, fact))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        null,
                        err -> log.error("[MemoryTools] Async save failed for user {}: {}", userId, err.getMessage())
                );

        return ""; // Return immediately — tool result is not surfaced to the user
    }
}
