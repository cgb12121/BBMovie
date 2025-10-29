package com.bbmovie.ai_assistant_service.core.low_level._repository;

import com.bbmovie.ai_assistant_service.core.low_level._entity._ChatHistory;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository("_ChatHistoryRepository")
public interface _ChatHistoryRepository extends R2dbcRepository<_ChatHistory, Long> {

}
