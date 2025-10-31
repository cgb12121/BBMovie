package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._SessionNotFoundException;
import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatSession;
import com.bbmovie.ai_assistant_service.core.low_level._repository._ChatSessionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.springframework.data.domain.Sort.*;
import static org.springframework.data.domain.Sort.Order.*;

@Slf4j
@Service
public class _ChatSessionService {

    private final _ChatSessionRepository repository;
    private final R2dbcEntityOperations template;

    @Autowired
    public _ChatSessionService(_ChatSessionRepository repository, @Qualifier("_EntityOperations") R2dbcEntityOperations template) {
        this.repository = repository;
        this.template = template;
    }

    public Flux<_ChatSession> findAll(int page, int size) {
        int offset = page * size;

        Query query = Query.empty()
                .sort(by(desc("update_at"))) //newest chat
                .limit(size)
                .offset(offset);

        return template.select(_ChatSession.class)
                .matching(query)
                .all()
                .log();
    }

    public Mono<_ChatSession> newSession(UUID userId, String sessionName) {
        _ChatSession newSession = _ChatSession.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionName(sessionName)
                .build();
        return repository.save(newSession)
                .log();
    }

    public Mono<Void> deleteSession(UUID sessionId) {
        return repository.findById(sessionId)
                .switchIfEmpty(Mono.error(new _SessionNotFoundException("Session not found")))
                .flatMap(session -> repository.deleteById(session.getId()))
                .log();
    }

    public Mono<_ChatSession> renameSession(UUID sessionId, String newName) {
        return repository.findById(sessionId)
                .switchIfEmpty(Mono.error(new _SessionNotFoundException("Session not found")))
                .flatMap(session -> {
                    session.setSessionName(newName);
                    return repository.save(session);
                })
                .log();
    }

    public Mono<Void> archiveSession(UUID sessionId) {
        return repository.findById(sessionId)
                .switchIfEmpty(Mono.error(new _SessionNotFoundException("Session not found")))
                .flatMap(session -> {
                    session.setArchived(!session.isArchived());
                    return repository.save(session);
                })
                .log()
                .then();
    }
}