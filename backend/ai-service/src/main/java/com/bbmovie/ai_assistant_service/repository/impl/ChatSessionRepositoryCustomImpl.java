package com.bbmovie.ai_assistant_service.repository.impl;

import com.bbmovie.ai_assistant_service.entity.ChatSession;
import com.bbmovie.ai_assistant_service.repository.ChatSessionRepositoryCustom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import java.time.Instant;
import java.util.UUID;

/**
 * This is the implementation of our custom repository interface.
 * Spring will find this class by its name and link it to _ChatSessionRepository.
 * <p>
 * All our complex query logic (using R2dbcEntityOperations) lives here.
 */
@Component
public class ChatSessionRepositoryCustomImpl implements ChatSessionRepositoryCustom {

    private final R2dbcEntityOperations r2dbcOperations;

    @Autowired
    public ChatSessionRepositoryCustomImpl(@Qualifier("entityOperations") R2dbcEntityOperations r2dbcOperations) {
        this.r2dbcOperations = r2dbcOperations;
    }

    @Override
    public Flux<ChatSession> findActiveSessionsWithCursor(UUID userId, Instant cursorTime, int limit) {
        Criteria criteria = Criteria
                .where("user_id").is(userId)
                .and("updated_at").lessThan(cursorTime)
                .and("is_archived").isFalse();

        Sort sort = Sort.by(Sort.Order.desc("updated_at"));
        Query query = Query.query(criteria)
                .sort(sort)
                .limit(limit);

        return r2dbcOperations
                .select(ChatSession.class)
                .matching(query)
                .all();
    }

    @Override
    public Flux<ChatSession> findArchivedSessionsWithCursor(UUID userId, Instant cursorTime, int limit) {
        Criteria criteria = Criteria.where("user_id").is(userId)
                .and("is_archived").isTrue()
                .and("updated_at").lessThan(cursorTime);

        Sort sort = Sort.by(Sort.Order.desc("updated_at"));
        Query query = Query.query(criteria)
                .sort(sort)
                .limit(limit);  // Fetch one extra

        return r2dbcOperations
                .select(ChatSession.class)
                .matching(query)
                .all();
    }
}