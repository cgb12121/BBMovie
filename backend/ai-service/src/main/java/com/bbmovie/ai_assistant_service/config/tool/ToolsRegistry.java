package com.bbmovie.ai_assistant_service.config.tool;

import com.bbmovie.ai_assistant_service.entity.model.AssistantType;
import com.bbmovie.ai_assistant_service.tool.AiTools;
import com.bbmovie.ai_assistant_service.utils.log.Logger;
import com.bbmovie.ai_assistant_service.utils.log.LoggerFactory;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolsRegistry {

    private static final Logger log = LoggerFactory.getLogger(ToolsRegistry.class);

    private final Map<String, ToolExecutor> executors;
    private final List<ToolSpecification> specifications;

    public ToolsRegistry(AssistantType assistantType, List<AiTools> tools) {
        this.executors = new HashMap<>();
        this.specifications = new ArrayList<>();
        discoverTools(assistantType, tools);
    }

    private void discoverTools(AssistantType assistantType, List<AiTools> toolBeans) {
        for (Object toolBean : toolBeans) {
            Class<?> toolClass = AopUtils.getTargetClass(toolBean);
            for (Method method : toolClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
                    this.specifications.add(spec);

                    ToolExecutor executor = new DefaultToolExecutor(toolBean, method);
                    this.executors.put(spec.name(), executor);
                }
            }
        }
        log.info("[{}] Discovered {} tools in total: {}", assistantType, this.executors.size(),this.executors.keySet());
    }

    public List<ToolSpecification> getToolSpecifications() {
        return specifications;
    }

    public ToolExecutor getExecutor(String toolName) {
        return executors.get(toolName);
    }
}