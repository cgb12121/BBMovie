package com.bbmovie.payment.utils;

import com.bbmovie.payment.dto.request.JwtDecodeResult;
import com.example.common.entity.JoseConstraint.JwtType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static com.example.common.entity.JoseConstraint.JosePayload.SUB;
import static com.example.common.entity.JoseConstraint.JwtType.JWE;
import static com.example.common.entity.JoseConstraint.JwtType.JWS;

public class SimpleJwtDecoder {

    private SimpleJwtDecoder() {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    public static String getUserId(String token) {
        JsonNode payload = decode(token);
        return payload.get(SUB).asText();
    }

    public static JwtDecodeResult getUserIdAndEmail(String token) {
        JsonNode payload = decode(token);
        String userId = payload.get(SUB).asText();
        String email = payload.get("email").asText();
        return new JwtDecodeResult(userId, email);
    }

    public static JsonNode decode(String token) {
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        JwtType type = JwtType.getType(token);
        if (type == null) {
            throw new IllegalArgumentException("Unable to determine JWT type");
        }

        String rawPayload = JwtType.getPayload(token);
        if (!StringUtils.hasText(rawPayload)) {
            throw new IllegalArgumentException("Payload must not be null or empty");
        }

        try {
            if (JWS.equals(type)) {
                return decodeJwsPayload(rawPayload);
            }

            if (JWE.equals(type)) {
                String jwePayload = decodeJwePayload(token);

                if (!StringUtils.hasText(jwePayload)) {
                    throw new IllegalArgumentException("JWE payload is empty");
                }

                return MAPPER.readTree(jwePayload);
            }
            throw new IllegalArgumentException("Invalid token format");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static JsonNode decodeJwsPayload(String payload) throws JsonProcessingException {
        byte[] decoded = Base64.getUrlDecoder().decode(payload);
        return MAPPER.readTree(new String(decoded, StandardCharsets.UTF_8));
    }

    private static String decodeJwePayload(String jwe) {
        return REST_TEMPLATE.getForObject("http://localhost:8080/jwe/decode", String.class, Map.of("token", jwe));
    }
}