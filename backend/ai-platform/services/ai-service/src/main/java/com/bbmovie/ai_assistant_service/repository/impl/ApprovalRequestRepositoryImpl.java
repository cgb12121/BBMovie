package com.bbmovie.ai_assistant_service.repository.impl;

import com.bbmovie.ai_assistant_service.entity.ApprovalRequest;
import com.bbmovie.ai_assistant_service.repository.custom.ApprovalRequestCustomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ApprovalRequestRepositoryImpl implements ApprovalRequestCustomRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> createRequest(ApprovalRequest request) {
        String sql = """
            INSERT INTO approval_requests (
                id, approval_token, user_id, session_id, message_id, 
                action_type, risk_level, tool_name, payload, request_status, 
                created_at, expires_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(sql)
                .bind(0, request.getId())
                .bind(1, request.getApprovalToken())
                .bind(2, request.getUserId())
                .bind(3, request.getSessionId());

        if (request.getMessageId() != null) {
            spec = spec.bind(4, request.getMessageId());
        } else {
            spec = spec.bindNull(4, String.class);
        }

        return spec
                .bind(5, request.getActionType())
                .bind(6, request.getRiskLevel())
                .bind(7, request.getToolName())
                .bind(8, request.getPayload())
                .bind(9, request.getStatus())
                .bind(10, request.getCreatedAt())
                .bind(11, request.getExpiresAt())
                .fetch()
                .rowsUpdated();
    }
}
