package com.bbmovie.ai_assistant_service._experimental._low_level.database;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository("experimentalChatHistoryRepository")
public interface _ChatHistoryRepository extends R2dbcRepository<_ChatHistory, Long> {

}
