package com.bbmovie.ai_assistant_service.core.high_level.controller;

import com.bbmovie.ai_assistant_service.core.high_level.agent.domain.entity.ChatSession;
import com.bbmovie.ai_assistant_service.dto.ApiResponse;
import com.bbmovie.ai_assistant_service.dto.CreateSessionRequest;
import com.bbmovie.ai_assistant_service.core.high_level.repository.ChatMessageRepository;
import com.bbmovie.ai_assistant_service.core.high_level.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/conversation")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<ChatSession>>>> getConversations() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMapMany(chatSessionRepository::findByUserId)
                .collectList()
                .map(sessions -> ResponseEntity.ok(ApiResponse.success(sessions)));
    }

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<ChatSession>>> createConversation(@RequestBody CreateSessionRequest request) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(userId -> {
                    ChatSession newSession = ChatSession.builder()
                            .userId(userId)
                            .sessionName(request.getSessionName())
                            .createdAt(LocalDateTime.now())
                            .build();
                    return chatSessionRepository.save(newSession);
                })
                .map(savedSession -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(savedSession)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> deleteConversation(@PathVariable String id) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication().getName())
                .flatMap(userId -> chatSessionRepository.findById(id)
                        .flatMap(session -> {
                            if (!session.getUserId().equals(userId)) {
                                return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(ApiResponse.<Void>error("You do not have permission to delete this session.")));
                            }
                            return chatMessageRepository.deleteBySessionId(session.getId())
                                    .then(chatSessionRepository.deleteById(session.getId()))
                                    .then(Mono.just(ResponseEntity.ok(ApiResponse.<Void>success(null))));
                        })
                        .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(ApiResponse.error("Session not found.")))
                );
    }
}
