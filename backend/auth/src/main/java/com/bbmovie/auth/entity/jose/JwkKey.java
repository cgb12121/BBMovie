package com.bbmovie.auth.entity.jose;

import com.bbmovie.auth.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jwk_keys")
public class JwkKey extends BaseEntity {
    private String kid;

    @Column(length = 4096, nullable = false)
    private String publicJwk;

    @Column(length = 4096)
    private String privateJwk;

    private boolean isActive = true;

    private Instant createdAt;

    @Override
    public String toString() {
        return "\n Jwk { \n"
                + "          kid: " + this.kid + ",\n"
                + "          public key: " + this.publicJwk + ",\n"
                + "          private key: [SECRET]"  + ", \n"
                + "          isActive: " + this.isActive + ", \n"
                + "          createdAt: " + this.createdAt +
                "\n }";
    }
}
