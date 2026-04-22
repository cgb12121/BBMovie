package com.bbmovie.ai_assistant_service.repository;

import com.bbmovie.ai_assistant_service.entity.ApprovalRequest;
import com.bbmovie.ai_assistant_service.repository.custom.ApprovalRequestCustomRepository;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface ApprovalRequestRepository extends ApprovalRequestCustomRepository, R2dbcRepository<ApprovalRequest, String> {
    @Query("SELECT * FROM approval_requests WHERE approval_token = :token AND session_id = :sessionId")
    Mono<ApprovalRequest> findByApprovalTokenAndSessionId(
            @Param("token") String token,
            @Param("sessionId") String sessionId
    );
}