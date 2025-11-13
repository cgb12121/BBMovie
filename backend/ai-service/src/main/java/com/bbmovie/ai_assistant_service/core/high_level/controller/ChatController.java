package com.bbmovie.ai_assistant_service.core.high_level.controller;

import com.bbmovie.ai_assistant_service.core.high_level.agent.AdminAssistant;
import com.bbmovie.ai_assistant_service.core.high_level.agent.UserAssistant;
import com.bbmovie.ai_assistant_service.core.high_level.dto.ChatRequest;
import com.bbmovie.common.dtos.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Collection;

import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ABAC.SUBSCRIPTION_TIER;
import static com.bbmovie.common.entity.JoseConstraint.JosePayload.ROLE;

@Slf4j
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
    public Flux<ApiResponse<String>> chat(@RequestBody ChatRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (request.getSessionId() == null) {
            return Flux.just(ApiResponse.error("Error: sessionId cannot be null. Please start a new conversation."));
        }

        String tier = jwt.getClaimAsString(SUBSCRIPTION_TIER);
        String role = jwt.getClaimAsString(ROLE);

        return userAssistant.chat(request.getSessionId(), request.getMessage(), role, tier)
                .map(ApiResponse::success);
    }

    @PostMapping(value = "/admin", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ApiResponse<String>> adminChat(@RequestBody ChatRequest request, @AuthenticationPrincipal Jwt jwt) {
        if (request.getSessionId() == null) {
            return Flux.just(ApiResponse.error("Error: sessionId cannot be null. Please start a new conversation."));
        }

        String role = jwt.getClaimAsString(ROLE);

        return adminAssistant.chat(request.getSessionId(), request.getMessage(), role)
                .map(ApiResponse::success);
    }

    @GetMapping("/test")
    public Flux<String> test() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMapMany(authentication -> {
                    Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
                    return Flux.just(authorities.toString());
                });
    }
}
