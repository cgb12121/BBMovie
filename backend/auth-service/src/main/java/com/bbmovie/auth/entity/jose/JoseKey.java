package com.bbmovie.auth.entity.jose;

import com.bbmovie.auth.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@With
@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jwk_keys")
public class JoseKey extends BaseEntity {

    @JsonProperty("kid")
    private String kid;

    @JsonProperty("public_key")
    @Column(name = "public_jwk", length = 4096, nullable = false)
    private String publicJwk;

    @JsonIgnore
    @Column(name = "private_jwk", length = 4096, nullable = false)
    private String privateJwk;

    @JsonProperty("is_active")
    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

    @Override
    public String toString() {
        return "\nJwk { \n"
                + "     kid: '" + kid + ";\n"
                + "     publicJwk: '" + publicJwk + ";\n"
                + "     privateJwk: [SECRET] \n"
                + "     isActive: " + isActive + "\n"
                + "     createdDate: " + getCreatedDate() + "\n"
                + "     lastModifiedDate: " + getLastModifiedDate() + "\n"
                + "}";
    }
}