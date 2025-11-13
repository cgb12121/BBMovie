package com.bbmovie.ai_assistant_service.core.low_level._repository;

import com.bbmovie.ai_assistant_service.core.low_level._entity._AiInteractionAudit;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

@Repository("_AiInteractionAuditRepository")
public interface _AiInteractionAuditRepository extends R2dbcRepository<_AiInteractionAudit, Long> {
}
