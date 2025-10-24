package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.agent.AdminAssistant;
import com.bbmovie.ai_assistant_service.agent.UserAssistant;
import com.bbmovie.ai_assistant_service.dto.ApiResponse;
import com.bbmovie.ai_assistant_service.dto.ChatRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.bbmovie.ai_assistant_service.utils.JwtUtils.extractUserTier;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@RestController
public class ChatController {

    private final UserAssistant userAssistant;
    private final AdminAssistant adminAssistant;

    @Autowired
    public ChatController(UserAssistant userAssistant, AdminAssistant adminAssistant) {
        this.userAssistant = userAssistant;
        this.adminAssistant = adminAssistant;
    }

    @PostMapping(value = "/chat", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<ApiResponse<String>> userAssistant(@RequestHeader("ID") String userId, @RequestBody ChatRequest request) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMapMany(authentication -> {
                String userTier = extractUserTier(authentication);
                return userAssistant.chat(userId, request.getMessage(), userTier)
                    .map(ApiResponse::success)
                    .onErrorResume(ignored -> Mono.just(ApiResponse.error("Unexpected error happened, please try again later.")));
            });
    }

    @PostMapping(value = "/admin/chat", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<ApiResponse<String>> adminAssistant(@RequestHeader("ID") String userId, @RequestBody ChatRequest request) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .flatMapMany(authentication -> {
                String userTier = extractUserTier(authentication);
                return adminAssistant.chat(userId, request.getMessage(), userTier)
                    .map(ApiResponse::success)
                    .onErrorResume(ignored -> Mono.just(ApiResponse.error("Unexpected error happened, please try again later.")));
            });
    }
}