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

    /**
     *
     * @param userRole Role from JWT will be in uppercase, it is usually like in JWT: "role": "ADMIN"
     * @return the AI assistant for the role
     */
    public static AssistantType fromUserRole(String userRole) {
        return switch (userRole) {
            case "ADMIN", "ROLE_ADMIN" -> AssistantType.ADMIN;
            case "MOD", "ROLE_MOD" -> AssistantType.MOD;
            case "USER", "ROLE_USER" -> AssistantType.USER;
            case null, default -> AssistantType.ANONYMOUS;
        };
    }
}
