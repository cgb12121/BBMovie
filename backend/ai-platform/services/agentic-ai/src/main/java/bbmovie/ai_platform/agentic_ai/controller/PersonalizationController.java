package bbmovie.ai_platform.agentic_ai.controller;

import bbmovie.ai_platform.agentic_ai.dto.request.UpdateAgentConfigDto;
import bbmovie.ai_platform.agentic_ai.service.PersonalizationService;
import lombok.RequiredArgsConstructor;

import com.bbmovie.common.dtos.ApiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/personalization")
public class PersonalizationController {

    private final PersonalizationService personalizationService;

    @GetMapping("/instructions")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<String>> getCustomInstructions() {
        UUID mockUserId = UUID.randomUUID(); // Placeholder for Auth Context
        return personalizationService.getCustomInstructions(mockUserId)
                .map(ApiResponse::success);
    }

    @PutMapping("/instructions")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<String>> updateCustomInstructions(@RequestBody UpdateAgentConfigDto dto) {
        UUID mockUserId = UUID.randomUUID(); 
        return personalizationService.updateCustomInstructions(mockUserId, dto)
                .then(Mono.fromCallable(() -> ApiResponse.success("Custom instructions updated")));
    }
}
