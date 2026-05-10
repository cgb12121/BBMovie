package bbmovie.ai_platform.agentic_ai.tool;

import bbmovie.ai_platform.agentic_ai.entity.AgentMemory;
import bbmovie.ai_platform.agentic_ai.repository.MemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import bbmovie.ai_platform.aop_policy.annotation.Monitored;
import org.springframework.context.annotation.Description;
import org.springframework.ai.chat.model.ToolContext;
import java.util.function.BiFunction;
import java.util.UUID;

@Configuration
public class MemoryTools {

    private static final Logger log = LoggerFactory.getLogger(MemoryTools.class);
    private final MemoryRepository memoryRepository;

    public MemoryTools(MemoryRepository memoryRepository) {
        this.memoryRepository = memoryRepository;
    }

    public record SaveMemoryRequest(String fact) {}
    public record SaveMemoryResponse(String result) {}

    @Bean
    @Description("Saves an important fact about the user into their long-term memory across sessions.")
    @Monitored
    public BiFunction<SaveMemoryRequest, ToolContext, SaveMemoryResponse> saveMemory() {
        return (request, context) -> {
            log.info("Executing saveMemory tool: {}", request.fact());
            UUID userId = (UUID) context.getContext().get("userId");
            AgentMemory memory = AgentMemory.builder()
                    .id(UUID.randomUUID())
                    .userId(userId)
                    .fact(request.fact())
                    .build();
            memoryRepository.save(memory).subscribe();
            return new SaveMemoryResponse("Memory saved successfully");
        };
    }
    
    public record UpdateMemoryRequest(UUID memoryId, String newFact) {}
    public record UpdateMemoryResponse(String result) {}

    @Bean
    @Description("Updates an existing fact in the user's long-term memory.")
    @Monitored
    public BiFunction<UpdateMemoryRequest, ToolContext, UpdateMemoryResponse> updateMemory() {
        return (request, context) -> {
            log.info("Executing updateMemory tool: ID {}, Fact {}", request.memoryId(), request.newFact());
            UUID userId = (UUID) context.getContext().get("userId");
            memoryRepository.findById(request.memoryId())
                    .filter(memory -> memory.getUserId().equals(userId))
                    .flatMap(memory -> {
                        memory.setFact(request.newFact());
                        return memoryRepository.save(memory);
                    }).subscribe();
            return new UpdateMemoryResponse("Memory updated successfully");
        };
    }
}
