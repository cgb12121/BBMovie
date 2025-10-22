package com.bbmovie.ai_assistant_service.security.restriction.tool;

import dev.langchain4j.service.tool.ToolExecutor;

public record SecureTool(
    String name,
    String description,
    ToolAccess access, // PUBLIC or ADMIN
    ToolExecutor executor
) {}