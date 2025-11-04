package com.bbmovie.ai_assistant_service.core.low_level._config;

import com.bbmovie.ai_assistant_service.core.low_level._tool._AiTools;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class _ToolsRegistry {

    private final Map<String, ToolExecutor> executors;
    private final List<ToolSpecification> specifications;

    public _ToolsRegistry(List<_AiTools> tools) {
        this.executors = new HashMap<>();
        this.specifications = new ArrayList<>();
        discoverTools(tools);
    }

    private void discoverTools(List<_AiTools> toolBeans) {
        for (Object toolBean : toolBeans) {
            Class<?> toolClass = AopUtils.getTargetClass(toolBean);
            for (Method method : toolClass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    ToolSpecification spec = ToolSpecifications.toolSpecificationFrom(method);
                    this.specifications.add(spec);

                    ToolExecutor executor = new DefaultToolExecutor(toolBean, method);
                    this.executors.put(spec.name(), executor);
                    log.info("Discovered tool: name='{}', class='{}', method='{}'",
                            spec.name(), toolClass.getSimpleName(), method.getName());
                }
            }
        }
        log.info("Discovered {} tools in total: {}", this.executors.size(), executors.keySet());
    }

    public List<ToolSpecification> getToolSpecifications() {
        return specifications;
    }

    public ToolExecutor getExecutor(String toolName) {
        return executors.get(toolName);
    }
}
