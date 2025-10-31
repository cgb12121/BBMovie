package com.bbmovie.ai_assistant_service.core.low_level._entity._model;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum AssistantType {
    ADMIN("admin"),
    USER("user");

    private final String code;

    AssistantType(String code) {
        this.code = code;
    }

    public static AssistantType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown assistant type code: " + code));
    }
}
