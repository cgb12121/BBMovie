package com.bbmovie.ai_assistant_service.repository.impl;

import com.bbmovie.ai_assistant_service.entity.ChatMessage;
import com.bbmovie.ai_assistant_service.jooq.generated.Tables;
import com.bbmovie.ai_assistant_service.repository.ChatMessageCustomRepository;
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
public class ChatMessageRepositoryImpl implements ChatMessageCustomRepository {

    private final DSLContext dsl;

    @Override
    public Flux<ChatMessage> getWithCursor(UUID sessionId, Instant cursorTime, int size) {
        var cm = Tables.CHAT_MESSAGE;

        return Flux.from(
            dsl.selectFrom(cm)
               .where(cm.SESSION_ID.eq(sessionId.toString()))
               .and(cm.TIMESTAMP.lt(LocalDateTime.ofInstant(cursorTime, ZoneOffset.UTC)))
               .orderBy(cm.TIMESTAMP.desc())
               .limit(size + 1)
        ).map(record -> record.into(ChatMessage.class));
    }
}