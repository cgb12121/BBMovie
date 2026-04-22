package com.bbmovie.ai_assistant_service.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
public class RefineryResponse<T> {
    private boolean success;
    private T data;
    private String message;
    private Map<String, String> errors;

    @JsonProperty("error") // Fallback for legacy error format if any
    private String legacyError;

    public String getErrorSummary() {
        if (errors != null && !errors.isEmpty()) {
            return errors.toString();
        }
        if (message != null) {
            return message;
        }
        return legacyError;
    }
}
