package bbmovie.ai_platform.agentic_ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("agent_personalization")
public class AgentPersonalization {

    @Id
    @Column("user_id")
    private UUID userId;

    @Column("custom_instructions")
    private String customInstructions;

    @Column("tone")
    private String tone;
}
