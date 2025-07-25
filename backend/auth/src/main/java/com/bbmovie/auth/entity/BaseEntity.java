package com.bbmovie.auth.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty("id")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @JsonProperty("created_date")
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @JsonProperty("last_modified_date")
    @Column(name = "last_modified_date", nullable = false)
    private LocalDateTime lastModifiedDate;

    @Version
    @JsonProperty("version")
    @Column(name = "version", nullable = false)
    private Long version;
}