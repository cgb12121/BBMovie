package com.bbmovie.ai_assistant_service.core.low_level._config._tool;

import com.bbmovie.ai_assistant_service.core.low_level._entity._model._AssistantType;
import com.bbmovie.ai_assistant_service.core.low_level._tool._AiTools;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._Logger;
import com.bbmovie.ai_assistant_service.core.low_level._utils._log._LoggerFactory;
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

public class _ToolsRegistry {

    private static final _Logger log = _LoggerFactory.getLogger(_ToolsRegistry.class);

    private final Map<String, ToolExecutor> executors;
    private final List<ToolSpecification> specifications;

    public _ToolsRegistry(_AssistantType assistantType, List<_AiTools> tools) {
        this.executors = new HashMap<>();
        this.specifications = new ArrayList<>();
        discoverTools(assistantType, tools);
    }

    private void discoverTools(_AssistantType assistantType, List<_AiTools> toolBeans) {
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