package bbmovie.commerce.entitlement_service.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "plan_content_policies")
public class PlanContentPolicyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false, length = 128)
    private String planId;

    @Column(name = "content_package", nullable = false, length = 128)
    private String contentPackage;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;
}
