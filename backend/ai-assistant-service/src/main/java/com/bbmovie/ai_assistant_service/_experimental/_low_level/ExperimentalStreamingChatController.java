package com.bbmovie.ai_assistant_service._experimental._low_level;

import com.bbmovie.ai_assistant_service.dto.ChatRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/experimental")
@RequiredArgsConstructor
public class ExperimentalStreamingChatController {

    private final ExperimentalStreamingUserAssistant streamingUserAssistant;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        return streamingUserAssistant.chat(request.getSessionId(), request.getMessage(), "ADMIN")
                .onErrorResume(e -> Flux.just("[error] " + e.getMessage()));
    }
}
