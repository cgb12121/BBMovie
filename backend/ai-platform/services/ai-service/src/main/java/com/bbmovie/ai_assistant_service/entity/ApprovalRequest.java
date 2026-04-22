package com.bbmovie.ai_assistant_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("approval_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequest {

    @Id
    @Column("id")
    private String id;

    @Column("approval_token")
    private String approvalToken;

    @Column("user_id")
    private String userId;

    @Column("session_id")
    private String sessionId;

    @Column("message_id")
    private String messageId;

    @Column("action_type")
    private String actionType;

    @Column("risk_level")
    private String riskLevel;

    @Column("tool_name")
    private String toolName;

    @Column("payload")
    private String payload;

    @Column("request_status") // Renamed column
    private String status;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("expires_at")
    private LocalDateTime expiresAt;

    @Column("approved_at")
    private LocalDateTime approvedAt;

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED,
        EXECUTED
    }
}