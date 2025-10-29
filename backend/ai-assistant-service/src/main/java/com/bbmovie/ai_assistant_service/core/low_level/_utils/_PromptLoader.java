package com.bbmovie.ai_assistant_service.core.low_level._utils;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class _PromptLoader {

    private static final String PROMPT_BASE_PATH = "prompts/";
    private static final String SYSTEM_DIR = PROMPT_BASE_PATH + "system/";
    private static final String PERSONAL_DIR = PROMPT_BASE_PATH + "personal/";
    private static final String TOOL_DIR = PROMPT_BASE_PATH + "tool-instruction/";

    private _PromptLoader() {}

    public static SystemMessage loadSystemPrompt(
            @NonNull Boolean enablePersona, @Nullable _AiModel model, @Nullable Map<String, Object> vars) {

        String generalSystemPrompt = loadText(SYSTEM_DIR + "system-prompt.txt");
        String securityPolicyPrompt = loadText(SYSTEM_DIR + "security-policy.txt");
        String personaPrompt = "";

        if (enablePersona && model != null) {
            personaPrompt = loadText(PERSONAL_DIR + model.getPersonaPromptPath());
        }

        String combined = String.join("\n\n---\n\n", generalSystemPrompt, securityPolicyPrompt, personaPrompt).trim();

        vars = (vars == null) ? Map.of() : vars;
        String rendered = PromptTemplate.from(combined).apply(vars).text();
        return SystemMessage.from(rendered);
    }

    public static String loadText(@NonNull String filePath) {
        try {
            ClassPathResource resource = new ClassPathResource(filePath);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load prompt: " + filePath, e);
        }
    }
}