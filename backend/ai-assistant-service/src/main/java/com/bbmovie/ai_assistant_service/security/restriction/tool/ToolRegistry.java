package com.bbmovie.ai_assistant_service.security.restriction.tool;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {
    private final Map<String, SecureTool> tools = new HashMap<>();

    public ToolRegistry() {
        tools.put("movie", new SecureTool("movie", "Search movies", ToolAccess.PUBLIC, this::searchMovies));
        tools.put("userStats", new SecureTool("userStats", "Admin user stats", ToolAccess.ADMIN, this::getUserStats));
    }

    public List<ToolSpecification> getToolsFor(String role) {
        return tools.values().stream()
            .filter(t -> t.access() == ToolAccess.PUBLIC || "admin".equalsIgnoreCase(role))
            .map(t -> ToolSpecification.builder()
                .name(t.name())
                .description(t.description())
                .parameters(JsonObjectSchema.builder()
                        .build())
                .build())
            .toList();
    }

    public Object execute(String toolName, ToolExecutionRequest request, String role) {
        SecureTool tool = tools.get(toolName);
        if (tool == null) throw new SecurityException("Unknown tool");
        if (tool.access() == ToolAccess.ADMIN && !"admin".equalsIgnoreCase(role))
            throw new SecurityException("Access denied");
        return tool.executor().execute(request, null);
    }
}