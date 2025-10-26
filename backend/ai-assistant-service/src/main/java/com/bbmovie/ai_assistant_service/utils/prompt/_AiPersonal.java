package com.bbmovie.ai_assistant_service.utils.prompt;

import lombok.Getter;

@Getter
public enum _AiPersonal {
    QWEN("qwen.txt");

    private final String fileName;

    _AiPersonal(String fileName) {
        this.fileName = fileName;
    }
}
