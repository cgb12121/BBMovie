package com.bbmovie.auth.entity.jose;

import com.bbmovie.auth.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.*;
import lombok.*;

@With
@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jwk_keys")
public class JwkKey extends BaseEntity {

    public enum KeyType {
        RSA, HMAC
    }

    @JsonProperty("kid")
    private String kid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private KeyType keyType = KeyType.RSA;

    @JsonProperty("public_key")
    @Column(length = 4096)
    private String publicJwk; // Used for RSA public key

    @JsonIgnore
    @Column(length = 4096)
    private String privateJwk; // Used for RSA private key

    @JsonIgnore
    @Column(length = 1024)
    private String hmacSecret; // Used for HMAC secret key

    @JsonProperty("is_active")
    @Builder.Default
    private boolean isActive = true;

    @Override
    public String toString() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        try {
            ObjectNode jwkNode = mapper.valueToTree(this);
            if (publicJwk != null) {
                JsonNode publicJwkNode = mapper.readTree(this.publicJwk);
                jwkNode.set("publicJwk", publicJwkNode);
            }
            jwkNode.put("privateJwk", "[SECRET]");
            jwkNode.put("hmacSecret", "[SECRET]");
            return mapper.writeValueAsString(jwkNode);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return "\nJwk { \n"
                    + "          kid: " + this.kid + ",\n"
                    + "          keyType: " + this.keyType + ",\n"
                    + "          public key: " + this.publicJwk + ",\n"
                    + "          private key: [SECRET]" + ", \n"
                    + "          hmacSecret: [SECRET]" + ", \n"
                    + "          isActive: " + this.isActive + ", \n"
                    + "          createdAt: " + this.getCreatedDate() +
                    "\n }";
        }
    }
}
