package com.example.bbmovie.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlindResult<T> {
    private boolean success;
    private T data;
    private Map<String, String> errors;
    private String message;

    public static <T> BlindResult<T> success(T data) {
        return new BlindResult<>(true, data, null, null);
    }

    public static <T> BlindResult<T> success(T data, String message) {
        return new BlindResult<>(true, data, null, message);
    }

    public static <T> BlindResult<T> error(String message) {
        return new BlindResult<>(false, null, null, message);
    }

    public static <T> BlindResult<T> validationError(Map<String, String> errors) {
        return new BlindResult<>(false, null, errors, "Validation failed");
    }

    public void addError(String field, String message) {
        if (errors == null) {
            errors = new HashMap<>();
        }
        errors.put(field, message);
    }
} 