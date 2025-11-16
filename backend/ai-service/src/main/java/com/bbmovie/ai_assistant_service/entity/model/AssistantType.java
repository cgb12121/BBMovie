package com.bbmovie.ai_assistant_service.entity.model;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum AssistantType {
    ADMIN("admin"),
    MOD("mod"),
    USER("user"),
    ANONYMOUS("anonymous");

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
