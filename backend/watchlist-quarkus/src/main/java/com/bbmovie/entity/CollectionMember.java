package com.bbmovie.entity;

import com.bbmovie.entity.enums.MemberRole;
import com.bbmovie.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "collection_member", uniqueConstraints = @UniqueConstraint(columnNames = {"collection_id", "member_user_id"}))
public class CollectionMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private WatchlistCollection collection;

    @Column(name = "member_user_id", nullable = false)
    private UUID memberUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberRole role = MemberRole.VIEWER;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();
}


