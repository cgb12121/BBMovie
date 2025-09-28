package com.bbmovie.entity;

import com.bbmovie.entity.enums.InviteStatus;
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
@Table(name = "collection_invite")
public class CollectionInvite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_id", nullable = false)
    private WatchlistCollection collection;

    @Column(name = "inviter_user_id", nullable = false)
    private UUID inviterUserId;

    @Column(name = "invitee_user_id", nullable = false)
    private UUID inviteeUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_role", nullable = false, length = 20)
    private MemberRole requestedRole = MemberRole.VIEWER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InviteStatus status = InviteStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}


