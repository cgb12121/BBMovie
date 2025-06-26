package com.example.bbmovie.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.bbmovie.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

public class ValidationHandler {

    private ValidationHandler() {
        
    }

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

    public static <T> ResponseEntity<ApiResponse<T>> handleBindingErrors(BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(ApiResponse.validationError(
                    processValidationErrors(bindingResult.getAllErrors())));
        }
        return null;
    }
}