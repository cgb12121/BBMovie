package bbmovie.ai_platform.agentic_ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("agent_memory")
public class AgentMemory {

    @Id
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("fact")
    private String fact;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;
}
