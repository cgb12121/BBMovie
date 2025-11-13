package com.bbmovie.ai_assistant_service.core.low_level._entity._model;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum _AssistantType {
    ADMIN("admin"),
    MOD("mod"),
    USER("user"),
    ANONYMOUS("anonymous");

    private final String code;

    _AssistantType(String code) {
        this.code = code;
    }

    public static _AssistantType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown assistant type code: " + code));
    }
}
