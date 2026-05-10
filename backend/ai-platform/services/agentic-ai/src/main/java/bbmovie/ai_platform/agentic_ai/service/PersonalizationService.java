package bbmovie.ai_platform.agentic_ai.service;

import bbmovie.ai_platform.agentic_ai.dto.request.UpdateAgentConfigDto;
import bbmovie.ai_platform.agentic_ai.entity.AgentPersonalization;
import bbmovie.ai_platform.agentic_ai.repository.PersonalizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonalizationService {

    private final PersonalizationRepository personalizationRepository;

    public Mono<String> getCustomInstructions(UUID userId) {
        return personalizationRepository.findById(userId)
                .map(AgentPersonalization::getCustomInstructions)
                .defaultIfEmpty(""); 
    }

    public Mono<Void> updateCustomInstructions(UUID userId, UpdateAgentConfigDto dto) {
        return personalizationRepository.findById(userId)
                .defaultIfEmpty(new AgentPersonalization(userId, "", ""))
                .flatMap(entity -> {
                    entity.setCustomInstructions(dto.customInstructions());
                    entity.setTone(dto.tone());
                    return personalizationRepository.save(entity);
                })
                .then();
    }
}
