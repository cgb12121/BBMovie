package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._assistant._Assistant;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model.AssistantType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.common.entity.JoseConstraint.JosePayload.ROLE;
import static com.example.common.entity.JoseConstraint.JosePayload.SUB;

@Slf4j
@Service
public class _ChatService {

    private final Map<AssistantType, _Assistant> assistants;
    private final _SessionService sessionService;

    @Autowired
    public _ChatService(List<_Assistant> assistantList, _SessionService sessionService) {
        this.assistants = assistantList.stream().collect(Collectors.toUnmodifiableMap(_Assistant::getType, Function.identity()));
        this.sessionService = sessionService;
        log.info("Initialized ChatService with assistants: {}", this.assistants.keySet());
    }

    public Flux<String> chat(UUID sessionId, String message, AssistantType assistantType, Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        String userRole = jwt.getClaimAsString(ROLE);

        return sessionService.getAndValidateSessionOwnership(sessionId, userId)
                .flatMapMany(session -> {
                    _Assistant assistant = assistants.get(assistantType);
                    if (assistant == null) {
                        return Flux.error(new IllegalArgumentException("Unknown assistant type: " + assistantType));
                    }
                    return assistant.processMessage(sessionId, message, userRole);
                });
    }
}
