package com.bbmovie.ai_assistant_service.repository.impl;

import com.bbmovie.ai_assistant_service.entity.ChatSession;
import com.bbmovie.ai_assistant_service.repository.custom.ChatSessionRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryImpl implements ChatSessionRepositoryCustom {

    private static final Table<Record> CHAT_SESSION = DSL.table(DSL.name("chat_session"));
    private static final Field<String> CS_USER_ID = DSL.field(DSL.name("user_id"), SQLDataType.VARCHAR(36));
    private static final Field<LocalDateTime> CS_UPDATED_AT = DSL.field(DSL.name("updated_at"), SQLDataType.LOCALDATETIME(6));
    private static final Field<Boolean> CS_IS_ARCHIVED = DSL.field(DSL.name("is_archived"), SQLDataType.BOOLEAN);

    private final DSLContext dsl;

    @Override
    public Flux<ChatSession> findActiveSessionsWithCursor(UUID userId, Instant cursorTime, int limit) {
        return Flux.from(
            dsl.selectFrom(CHAT_SESSION)
               .where(CS_USER_ID.eq(userId.toString()))
               .and(CS_UPDATED_AT.lt(LocalDateTime.ofInstant(cursorTime, ZoneOffset.UTC)))
               .and(CS_IS_ARCHIVED.isFalse())
               .orderBy(CS_UPDATED_AT.desc())
               .limit(limit)
        ).map(record -> record.into(ChatSession.class));
    }

    @Override
    public Flux<ChatSession> findArchivedSessionsWithCursor(UUID userId, Instant cursorTime, int limit) {
        return Flux.from(
            dsl.selectFrom(CHAT_SESSION)
               .where(CS_USER_ID.eq(userId.toString()))
               .and(CS_IS_ARCHIVED.isTrue())
               .and(CS_UPDATED_AT.lt(LocalDateTime.ofInstant(cursorTime, ZoneOffset.UTC)))
               .orderBy(CS_UPDATED_AT.desc())
               .limit(limit)
        ).map(record -> record.into(ChatSession.class));
    }
}
