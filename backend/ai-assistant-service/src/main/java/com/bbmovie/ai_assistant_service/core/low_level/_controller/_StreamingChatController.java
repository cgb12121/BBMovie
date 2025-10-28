package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._assistant._AdminAssistant;
import com.bbmovie.ai_assistant_service.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import static com.example.common.entity.JoseConstraint.JosePayload.ROLE;

@RestController
@RequiredArgsConstructor
@RequestMapping("/experimental")
public class _StreamingChatController {

    private final _AdminAssistant streamingUserAssistant;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request, @AuthenticationPrincipal Jwt jwt) {
        return streamingUserAssistant.chat(request.getSessionId(), request.getMessage(), jwt.getClaimAsString(ROLE));
    }
}
