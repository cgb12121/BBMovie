package com.bbmovie.ai_assistant_service.repository;

import com.bbmovie.ai_assistant_service.entity.ChatSession;
import com.bbmovie.ai_assistant_service.repository.custom.ChatSessionRepositoryCustom;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChatSessionRepository extends ChatSessionRepositoryCustom, R2dbcRepository<ChatSession, UUID> {

}
