package com.bbmovie.ai_assistant_service.repository.impl;

import com.bbmovie.ai_assistant_service.entity.ChatSession;
import com.bbmovie.ai_assistant_service.jooq.generated.Tables;
import com.bbmovie.ai_assistant_service.repository.ChatSessionRepositoryCustom;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;


@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryImpl implements ChatSessionRepositoryCustom {

    private final DSLContext dsl;

    @Override
    public Flux<ChatSession> findActiveSessionsWithCursor(UUID userId, Instant cursorTime, int limit) {
        var cs = Tables.CHAT_SESSION;

        return Flux.from(
            dsl.selectFrom(cs)
               .where(cs.USER_ID.eq(userId.toString()))
               .and(cs.UPDATED_AT.lt(LocalDateTime.ofInstant(cursorTime, ZoneOffset.UTC)))
               .and(cs.IS_ARCHIVED.isFalse())
               .orderBy(cs.UPDATED_AT.desc())
               .limit(limit)
        ).map(record -> record.into(ChatSession.class));
    }

    @Override
    public Flux<ChatSession> findArchivedSessionsWithCursor(UUID userId, Instant cursorTime, int limit) {
        var cs = Tables.CHAT_SESSION;

        return Flux.from(
            dsl.selectFrom(cs)
               .where(cs.USER_ID.eq(userId.toString()))
               .and(cs.IS_ARCHIVED.isTrue())
               .and(cs.UPDATED_AT.lt(LocalDateTime.ofInstant(cursorTime, ZoneOffset.UTC)))
               .orderBy(cs.UPDATED_AT.desc())
               .limit(limit)
        ).map(record -> record.into(ChatSession.class));
    }
}