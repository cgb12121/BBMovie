package com.bbmovie.payment.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class PaymentProviderPayloadUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PaymentProviderPayloadUtil() {}

    public static String toJsonString(Object payload) {
        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize provider payload", e);
        }
    }

    public static <T> T stringToJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize provider payload", e);
        }
    }

    public static JsonNode stringToJsonNode(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize provider payload", e);
        }
    }
}
