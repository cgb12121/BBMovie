package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.agent.AdminAssistant;
import com.bbmovie.ai_assistant_service.agent.UserAssistant;
import com.bbmovie.ai_assistant_service.dto.ApiResponse;
import com.bbmovie.ai_assistant_service.dto.ChatRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.bbmovie.ai_assistant_service.utils.JwtUtils.extractUserTier;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final UserAssistant userAssistant;
    private final AdminAssistant adminAssistant;

    @Autowired
    public ChatController(UserAssistant userAssistant, AdminAssistant adminAssistant) {
        this.userAssistant = userAssistant;
        this.adminAssistant = adminAssistant;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ApiResponse<String>> chat(@RequestBody ChatRequest request) {
        if (request.getSessionId() == null) {
            return Flux.just(ApiResponse.error("Error: sessionId cannot be null. Please start a new conversation."));
        }

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth -> {
                    String userTier = extractUserTier(auth);

                    return userAssistant.chat(request.getSessionId(), request.getMessage(), userTier)
                            .map(ApiResponse::success)
                            .onErrorResume(e -> Mono.just(ApiResponse.error("Unexpected error happened, please try again later.")));
                });
    }

    @PostMapping(value = "/admin", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ApiResponse<String>> adminChat(@RequestBody ChatRequest request) {
        if (request.getSessionId() == null)
            return Flux.just(ApiResponse.error("Error: sessionId cannot be null. Please start a new conversation."));

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(auth ->
                        adminAssistant.chat(request.getSessionId(), request.getMessage(), extractUserTier(auth))
                                .map(ApiResponse::success)
                                .onErrorResume(e -> Mono.just(ApiResponse.error("Unexpected error happened.")))
                );
    }
}
