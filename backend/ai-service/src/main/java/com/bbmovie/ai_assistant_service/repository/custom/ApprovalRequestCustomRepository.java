package com.bbmovie.ai_assistant_service.repository.custom;

import com.bbmovie.ai_assistant_service.entity.ApprovalRequest;
import reactor.core.publisher.Mono;

public interface ApprovalRequestCustomRepository {
    Mono<Long> createRequest(ApprovalRequest request);
}
