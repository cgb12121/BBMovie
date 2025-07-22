package com.bbmovie.auth.entity.jose;

import com.bbmovie.auth.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jwk_keys")
public class JwkKey extends BaseEntity {
    @JsonProperty("kid")
    private String kid;

    @JsonProperty("public_key")
    @Column(length = 4096, nullable = false)
    private String publicJwk;

    @JsonIgnore
    @Column(length = 4096)
    private String privateJwk;

    @JsonProperty("is_active")
    private boolean isActive = true;

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            return mapper.writeValueAsString(this);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return "\n Jwk { \n"
                    + "          kid: " + this.kid + ",\n"
                    + "          public key: " + this.publicJwk + ",\n"
                    + "          private key: [SECRET]"  + ", \n"
                    + "          isActive: " + this.isActive + ", \n"
                    + "          createdAt: " + this.getCreatedDate() +
                    "\n }";
        }
    }
}
