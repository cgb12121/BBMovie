package com.bbmovie.ai_assistant_service.repository.impl;

import com.bbmovie.ai_assistant_service.entity.ChatMessage;
import com.bbmovie.ai_assistant_service.repository.ChatMessageCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChatMessageCustomRepositoryImpl implements ChatMessageCustomRepository {

    @Qualifier("entityOperations") private final R2dbcEntityOperations r2dbcOperations;

    @Override
    public Flux<ChatMessage> getWithCursor(UUID sessionId, Instant cursorTime, int size) {
        Criteria criteria = Criteria.where("session_id").is(sessionId)
                .and("timestamp").lessThan(cursorTime);

        Sort sort = Sort.by(Sort.Order.desc("timestamp"));
        Query query = Query.query(criteria)
                .sort(sort)
                .limit(size + 1);

        return r2dbcOperations
                .select(ChatMessage.class)
                .matching(query)
                .all();
    }
}
