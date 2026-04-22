package bbmovie.commerce.entitlement_service.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "entitlement_override_audit")
public class EntitlementOverrideAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor", nullable = false, length = 128)
    private String actor;
    @Column(name = "target_user_id", nullable = false, length = 128)
    private String targetUserId;
    @Column(name = "action", nullable = false, length = 32)
    private String action;
    @Column(name = "reason", length = 512)
    private String reason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
