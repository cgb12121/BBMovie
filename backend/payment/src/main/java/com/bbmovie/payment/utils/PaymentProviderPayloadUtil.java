package com.bbmovie.payment.utils;

import com.bbmovie.payment.dto.request.CallbackRequestContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;

import java.util.Map;

@SuppressWarnings("squid:S1118")
public final class PaymentProviderPayloadUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static CallbackRequestContext createContext(HttpServletRequest request, Map<String, String> params, HttpMethod method) {
        return CallbackRequestContext.builder()
                .httpServletRequest(request)
                .formParams(params)
                .httpMethod(method.name())
                .contentType(request.getContentType())
                .build();
    }

    public static CallbackRequestContext createContext(HttpServletRequest request,String payload, HttpMethod method, Map<String, String> headers) {
        return CallbackRequestContext.builder()
                .httpServletRequest(request)
                .headers(headers)
                .rawBody(payload)
                .httpMethod(method.name())
                .contentType(request.getContentType())
                .build();
    }

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