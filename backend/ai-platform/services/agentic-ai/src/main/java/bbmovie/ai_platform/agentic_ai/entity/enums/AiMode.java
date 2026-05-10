package bbmovie.ai_platform.agentic_ai.entity.enums;

import lombok.Getter;

@Getter
public enum AiMode {
    NORMAL("Balanced speed and quality"),
    THINKING("Enables reasoning trace (Chain of Thought)");

    private final String description;

    AiMode(String description) {
        this.description = description;
    }
}
