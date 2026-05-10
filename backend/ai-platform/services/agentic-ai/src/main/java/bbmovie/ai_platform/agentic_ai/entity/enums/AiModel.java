package bbmovie.ai_platform.agentic_ai.entity.enums;

import lombok.Getter;

@Getter
public enum AiModel {
    QWEN_TINY("qwen3:0.6b", false, false),
    QWEN_BALANCED("qwen3:1.7b", true, false),
    DEEPSEEK_R1("deepseek-r1:1.5b", true, true),
    LLAMA_3_2("llama3.2:3b", true, false);

    private final String value;
    private final boolean supportsTools;
    private final boolean supportsThinking;

    AiModel(String value, boolean supportsTools, boolean supportsThinking) {
        this.value = value;
        this.supportsTools = supportsTools;
        this.supportsThinking = supportsThinking;
    }
}
