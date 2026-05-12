package bbmovie.ai_platform.agentic_ai.entity.enums;

import lombok.Getter;

@Getter
public enum AiModel {
    QWEN_TINY("qwen3:0.6b", "ollama", false, false),
    QWEN_BALANCED("qwen3:1.7b", "ollama", true, false),
    DEEPSEEK_R1("deepseek-r1:1.5b", "ollama", true, true),
    LLAMA_3_2("llama3.2:3b", "ollama", true, false),
    
    // Google AI Studio
    GEMINI_1_5_FLASH("gemini-1.5-flash", "google", true, false),
    GEMINI_1_5_PRO("gemini-1.5-pro", "google", true, false),
    
    // Groq (LPU)
    GROQ_LLAMA_3_70B("llama3-70b-8192", "groq", true, false),
    GROQ_MIXTRAL_8X7B("mixtral-8x7b-32768", "groq", true, false);

    private final String value;
    private final String provider;
    private final boolean supportsTools;
    private final boolean supportsThinking;

    AiModel(String value, String provider, boolean supportsTools, boolean supportsThinking) {
        this.value = value;
        this.provider = provider;
        this.supportsTools = supportsTools;
        this.supportsThinking = supportsThinking;
    }
}
