package com.bbmovie.ai_assistant_service.service;

import com.bbmovie.ai_assistant_service.dto.AuditRecord;
import reactor.core.publisher.Mono;

public interface AuditService {
    Mono<Void> recordInteraction(AuditRecord record);
}
