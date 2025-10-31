package com.bbmovie.ai_assistant_service.core.low_level;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class _SessionNotFoundException extends RuntimeException {
    public _SessionNotFoundException(String sessionId) {
        super("Chat session not found with ID: " + sessionId);
    }
}