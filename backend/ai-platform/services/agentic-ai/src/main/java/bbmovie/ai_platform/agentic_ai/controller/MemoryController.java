package bbmovie.ai_platform.agentic_ai.controller;

import com.bbmovie.common.dtos.ApiResponse;

import bbmovie.ai_platform.agentic_ai.dto.MemoryRecord;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/memories")
public class MemoryController {

    @GetMapping
    public Flux<ApiResponse<MemoryRecord>> listMemories() {
        // Return placeholder list
        return Flux.empty();
    }

    public record EditMemoryDto(String newFact) {}

    @PutMapping("/{memoryId}")
    public Mono<ApiResponse<String>> editMemory(@PathVariable UUID memoryId, @RequestBody EditMemoryDto dto) {
        return Mono.just(ApiResponse.success("Memory updated"));
    }

    @DeleteMapping("/{memoryId}")
    public Mono<ApiResponse<String>> deleteMemory(@PathVariable UUID memoryId) {
        return Mono.just(ApiResponse.success("Memory deleted"));
    }
}
