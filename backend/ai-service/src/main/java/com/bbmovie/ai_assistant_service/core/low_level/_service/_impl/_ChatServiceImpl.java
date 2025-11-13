package com.bbmovie.ai_assistant_service.core.low_level._service._impl;

import com.bbmovie.ai_assistant_service.core.low_level._assistant._Assistant;
import com.bbmovie.ai_assistant_service.core.low_level._dto._response._ChatStreamChunk;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AiMode;
import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._service._ChatService;
import com.bbmovie.ai_assistant_service.core.low_level._service._SessionService;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ROLE;
import static com.bbmovie.common.entity.JoseConstraint.JosePayload.SUB;

@Service
public class _ChatServiceImpl implements _ChatService {

    private static final _Logger log = _LoggerFactory.getLogger(_ChatServiceImpl.class);

    private final Map<_AssistantType, _Assistant> assistants;
    private final _SessionService sessionService;

    @Autowired
    public _ChatServiceImpl(List<_Assistant> assistantList, _SessionService sessionService) {
        this.assistants = assistantList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        _Assistant::getType,
                        Function.identity())
                );
        this.sessionService = sessionService;
        log.info("Initialized ChatService with assistants: {}", this.assistants.keySet());
    }

    @Override
    public Flux<_ChatStreamChunk> chat(UUID sessionId, String message, _AiMode aiMode, _AssistantType assistantType, Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        String userRole = jwt.getClaimAsString(ROLE);

        return sessionService.getAndValidateSessionOwnership(sessionId, userId)
                .flatMapMany(session -> {
                    _Assistant assistant = assistants.get(assistantType);
                    if (assistant == null) {
                        return Flux.error(new IllegalArgumentException("Unknown assistant type: " + assistantType));
                    }

                    return assistant.processMessage(sessionId, message, aiMode, userRole)
                            .startWith(_ChatStreamChunk.system("Assistant is thinking..."))
                            .concatWithValues(_ChatStreamChunk.system("Done"));
                });
    }
}