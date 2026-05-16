package bbmovie.ai_platform.agentic_ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import bbmovie.ai_platform.agentic_ai.entity.enums.Sender;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_message")
public class ChatMessage implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column("session_id")
    private UUID sessionId;

    @Column("user_id")
    private UUID userId;

    @Column("sender_type")
    private Sender senderType;

    @Column("content")
    private String content;

    @Column("thinking")
    private String thinking;

    @Column("prompt_tokens")
    private Long promptTokens;

    @Column("completion_tokens")
    private Long completionTokens;

    @Column("parent_id")
    private UUID parentId;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @Transient
    private boolean isNew;

    @Override
    @Transient
    public boolean isNew() {
        return this.isNew || this.id == null;
    }

    public ChatMessage asNew() {
        this.isNew = true;
        return this;
    }

    @Override
    public UUID getId() {
        return this.id;
    }
}
