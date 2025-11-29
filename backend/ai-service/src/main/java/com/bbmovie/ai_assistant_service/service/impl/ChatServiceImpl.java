package com.bbmovie.ai_assistant_service.service.impl;

import com.bbmovie.ai_assistant_service.assistant.Assistant;
import com.bbmovie.ai_assistant_service.dto.ChatContext;
import com.bbmovie.ai_assistant_service.dto.request.ChatRequestDto;
import com.bbmovie.ai_assistant_service.dto.response.ChatStreamChunk;
import com.bbmovie.ai_assistant_service.entity.model.AiMode;
import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import com.bbmovie.ai_assistant_service.service.ChatService;
import com.bbmovie.ai_assistant_service.service.SessionService;
import com.bbmovie.ai_assistant_service.utils.log.RgbLogger;
import com.bbmovie.ai_assistant_service.utils.log.RgbLoggerFactory;
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
public class ChatServiceImpl implements ChatService {

    private static final RgbLogger log = RgbLoggerFactory.getLogger(ChatServiceImpl.class);

    private final Map<AssistantType, Assistant> assistants;
    private final SessionService sessionService;

    @Autowired
    public ChatServiceImpl(List<Assistant> assistantList, SessionService sessionService) {
        this.assistants = assistantList.stream()
                .collect(Collectors.toUnmodifiableMap(
                        Assistant::getType,
                        Function.identity())
                );
        this.sessionService = sessionService;
        log.info("Initialized ChatService with assistants: {}", this.assistants.keySet());
    }

    @Override
    public Flux<ChatStreamChunk> chat(UUID sessionId, ChatRequestDto request, Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaimAsString(SUB));
        String userRole = jwt.getClaimAsString(ROLE);

        return sessionService.getAndValidateSessionOwnership(sessionId, userId)
                .flatMapMany(session -> {
                    AssistantType assistantType = AssistantType.fromCode(request.getAssistantType());
                    Assistant assistant = assistants.get(assistantType);
                    if (assistant == null) {
                        return Flux.error(new IllegalArgumentException("Unknown assistant type: " + assistantType));
                    }

                    ChatContext chatContext = ChatContext.builder()
                            .sessionId(session.getId())
                            .message(request.getMessage())
                            .aiMode(request.getAiMode())
                            .userRole(userRole)
                            .fileReferences(request.getFileReferences())
                            .extractedFileContent(request.getExtractedFileContent())
                            .build();

                    if (request.getAiMode() == AiMode.FAST || request.getAiMode() == AiMode.NORMAL) {
                        return assistant.processMessage(chatContext);
                    }

                    return assistant.processMessage(chatContext)
                            .startWith(ChatStreamChunk.system("Assistant is thinking..."))
                            .concatWithValues(ChatStreamChunk.system("Done"));
                });
    }
}