package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.agent.AdminAssistant;
import com.bbmovie.ai_assistant_service.agent.UserAssistant;
import com.bbmovie.ai_assistant_service.dto.ChatRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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

    //add produces = TEXT_EVENT_STREAM_VALUE to enable stream
    @PostMapping(value = "/chat")
    public Flux<String> userAssistant(@RequestHeader("ID") String userId, @RequestBody ChatRequest request) {
        return userAssistant.chat(userId, request.getMessage());
    }

    @PostMapping(value = "/admin/chat")
    public Flux<String> adminAssistant(@RequestHeader("ID") String userId, @RequestBody ChatRequest request) {
        return adminAssistant.chat(userId, request.getMessage());
    }
}