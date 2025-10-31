package com.bbmovie.ai_assistant_service.core.low_level._model;

import java.util.Arrays;

public enum AssistantType {
    ADMIN("admin"),
    USER("user");

    private final String code;

    AssistantType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AssistantType fromCode(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown assistant type code: " + code));
    }
}
