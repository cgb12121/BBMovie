package com.bbmovie.ai_assistant_service.repository.impl;

import com.bbmovie.ai_assistant_service.entity.ChatMessage;
import com.bbmovie.ai_assistant_service.repository.custom.ChatMessageCustomRepository;
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
public class ChatMessageRepositoryImpl implements ChatMessageCustomRepository {

    private static final Table<Record> CHAT_MESSAGE = DSL.table(DSL.name("chat_message"));
    private static final Field<String> CM_SESSION_ID =
            DSL.field(DSL.name("session_id"), SQLDataType.VARCHAR(36));
    private static final Field<LocalDateTime> CM_TIMESTAMP =
            DSL.field(DSL.name("timestamp"), SQLDataType.LOCALDATETIME(6));

    private final DSLContext dsl;

    @Override
    public Flux<ChatMessage> getWithCursor(UUID sessionId, Instant cursorTime, int size) {
        return Flux.from(
            dsl.selectFrom(CHAT_MESSAGE)
               .where(CM_SESSION_ID.eq(sessionId.toString()))
               .and(CM_TIMESTAMP.lt(LocalDateTime.ofInstant(cursorTime, ZoneOffset.UTC)))
               .orderBy(CM_TIMESTAMP.desc())
               .limit(size + 1)
        ).map(record -> record.into(ChatMessage.class));
    }
}
