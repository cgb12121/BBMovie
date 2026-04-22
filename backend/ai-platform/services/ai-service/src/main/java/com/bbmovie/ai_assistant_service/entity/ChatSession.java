package com.bbmovie.ai_assistant_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.lang.NonNull;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("chat_session")
public class ChatSession implements Persistable<UUID> {

    @Id @NonNull
    private UUID id;

    @Column("user_id")
    private UUID userId;

    @Column("session_name")
    private String sessionName;

    @Column("is_archived")
    private boolean isArchived;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    @Transient // <-- Add a 'transient' (not saved to DB) flag
    private boolean isNew;

    @Override
    @Transient
    public boolean isNew() {
        return this.isNew; // Tell Spring if we are new
    }

    public ChatSession asNew() {
        this.isNew = true;
        return this;
    }

    @Override
    public @NonNull UUID getId() {
        return this.id;
    }
}
