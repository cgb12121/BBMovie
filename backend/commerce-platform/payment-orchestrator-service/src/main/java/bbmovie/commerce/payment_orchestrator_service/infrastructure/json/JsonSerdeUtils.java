package bbmovie.commerce.payment_orchestrator_service.infrastructure.json;

import tools.jackson.databind.ObjectMapper;

public final class JsonSerdeUtils {

    private JsonSerdeUtils() {
    }

    public static String write(ObjectMapper objectMapper, Object value, String errorMessage) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }

    public static <T> T read(ObjectMapper objectMapper, String json, Class<T> type, String errorMessage) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }
}

