package bbmovie.ai_platform.agentic_ai.entity;

import bbmovie.ai_platform.agentic_ai.entity.enums.ApprovalStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("approval_request")
public class ApprovalRequest {
    @Id
    private String requestId; // Short ID
    private String toolName;
    private String action;
    private String riskLevel; // Store as String for R2DBC compatibility if needed
    private String arguments; // JSON string
    private UUID userId;
    private UUID sessionId;
    private UUID messageId;
    private ApprovalStatus status; // PENDING, APPROVED, REJECTED
    private Instant createdAt;
    private Instant updatedAt;
}
