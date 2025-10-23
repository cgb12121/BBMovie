package com.bbmovie.ai_assistant_service.controller;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/conversation")
public class ConversationController {

    @GetMapping("/{id}")
    public Flux<String> conversation(@PathVariable String id) {
        return Flux.empty();
    }

    @DeleteMapping("/{id}")
    public Flux<String> deleteConversation(@PathVariable String id) {
        return Flux.empty();
    }

    @PostMapping("/{id}")
    public Flux<String> createConversation(@PathVariable String id, @RequestBody String message) {
        return Flux.empty();
    }
}
