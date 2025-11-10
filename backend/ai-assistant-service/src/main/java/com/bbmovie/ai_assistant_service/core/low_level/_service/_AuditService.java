package com.bbmovie.ai_assistant_service.core.low_level._service;

import com.bbmovie.ai_assistant_service.core.low_level._dto._AuditRecord;
import reactor.core.publisher.Mono;

public interface  _AuditService {
    Mono<Void> recordInteraction(_AuditRecord record);
}
