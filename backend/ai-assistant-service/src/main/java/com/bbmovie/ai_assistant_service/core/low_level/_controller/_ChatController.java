package com.bbmovie.ai_assistant_service.core.low_level._controller;

import com.bbmovie.ai_assistant_service.core.low_level._assistant._AdminAssistant;
import com.bbmovie.ai_assistant_service.dto.ChatRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import static com.example.common.entity.JoseConstraint.JosePayload.ROLE;

@RestController
@RequestMapping("/v0")
public class _ChatController {

    private final _AdminAssistant streamingUserAssistant;

    @Autowired
    public _ChatController(_AdminAssistant streamingUserAssistant) {
        this.streamingUserAssistant = streamingUserAssistant;
    }

    @PostMapping(value = "/chat")
    public Flux<String> streamChat(@RequestBody ChatRequest request, @AuthenticationPrincipal Jwt jwt) {
        return streamingUserAssistant.chat(request.getSessionId(), request.getMessage(), jwt.getClaimAsString(ROLE));
    }
}
