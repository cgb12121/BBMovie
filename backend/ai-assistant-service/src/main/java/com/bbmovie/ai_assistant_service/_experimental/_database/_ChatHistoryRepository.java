package com.bbmovie.ai_assistant_service._experimental._database;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository("experimentalChatHistoryRepository")
@ConditionalOnBooleanProperty(name = "ai.experimental.enabled")
public interface _ChatHistoryRepository extends R2dbcRepository<_ChatHistory, Long> {

}
