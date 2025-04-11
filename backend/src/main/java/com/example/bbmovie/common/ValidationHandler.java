package com.example.bbmovie.common;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ValidationHandler {
    public static Map<String, String> processValidationErrors(List<ObjectError> errors) {
        Map<String, String> validationErrors = new HashMap<>();

        for (ObjectError error : errors) {
            if (error instanceof FieldError fieldError) {
                validationErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
            } else {
                validationErrors.put(error.getObjectName(), error.getDefaultMessage());
            }
        }

        return validationErrors;
    }
}