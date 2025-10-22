package com.bbmovie.ai_assistant_service.controller;

import com.bbmovie.ai_assistant_service.agent.StreamingAssistant;
import com.bbmovie.ai_assistant_service.dto.ChatRequest;
import dev.langchain4j.data.message.AiMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Objects;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@RestController
@RequestMapping(value = "/chat")
public class AssistantController {

    private final StreamingAssistant streamingAssistant;

    @Autowired
    public AssistantController(StreamingAssistant streamingAssistant) {
        this.streamingAssistant = streamingAssistant;
    }

    @PostMapping(produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamingAssistant(@RequestBody ChatRequest request) {
        return streamingAssistant.chat(request.getMessage());
    }
}