package bbmovie.ai_platform.agentic_ai.tool;

import bbmovie.ai_platform.agentic_ai.entity.AgentMemory;
import bbmovie.ai_platform.agentic_ai.repository.MemoryRepository;
import bbmovie.ai_platform.aop_policy.annotation.Monitored;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class MemoryTools {

    private static final Logger log = LoggerFactory.getLogger(MemoryTools.class);
    private final MemoryRepository memoryRepository;

    public MemoryTools(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    @Tool(description = "Saves an important fact about the user into their long-term memory across sessions.")
    @Monitored
    public String saveMemory(
            @ToolParam(description = "The specific fact or information about the user to remember") String fact,
            ToolContext toolContext
    ) {
        log.info("Executing saveMemory tool: {}", fact);
        UUID userId = (UUID) toolContext.getContext().get("userId");
        
        AgentMemory memory = AgentMemory.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .fact(fact)
                .build();
        
        memoryRepository.save(memory).subscribe();
        return "Memory saved successfully: " + fact;
    }

    @Tool(description = "Updates an existing fact in the user's long-term memory.")
    @Monitored
    public String updateMemory(
            @ToolParam(description = "The unique ID of the memory to update") UUID memoryId,
            @ToolParam(description = "The new fact content to replace the old one") String newFact,
            ToolContext toolContext
    ) {
        log.info("Executing updateMemory tool: ID {}, Fact {}", memoryId, newFact);
        UUID userId = (UUID) toolContext.getContext().get("userId");
        
        memoryRepository.findById(memoryId)
                .filter(memory -> memory.getUserId().equals(userId))
                .flatMap(memory -> {
                    memory.setFact(newFact);
                    return memoryRepository.save(memory);
                }).subscribe();
                
        return "Memory updated successfully for ID: " + memoryId;
    }
}
