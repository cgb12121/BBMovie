package bbmovie.ai_platform.agentic_ai.service.tool;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.method.MethodToolCallback;
import org.springframework.ai.tool.support.ToolDefinitions;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * ToolManager aggregates and manages AI tools from multiple sources:
 * 1. Internal tools: Methods annotated with {@link Tool}.
 * 2. External tools: Provided via Model Context Protocol (MCP) or other {@link ToolCallbackProvider}s.
 * 
 * It features a caching mechanism to avoid redundant discovery and heavy calls to external providers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolManager {
    
    private final List<ToolCallbackProvider> providers;
    private final ApplicationContext applicationContext;
    private final AtomicReference<List<ToolCallback>> cachedTools = new AtomicReference<>(new ArrayList<>());
    private final List<ToolCallback> discoveredLocalTools = new ArrayList<>();

    /**
     * Automatically refreshes the tool list once the application is ready.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready. Discovering local tools and initializing tool cache...");
        discoverLocalTools();
        refreshTools();
    }

    /**
     * Scans all beans in the application context for methods annotated with {@link Tool}.
     * Discovered tools are wrapped in {@link MethodToolCallback} for Spring AI compatibility.
     */
    private void discoverLocalTools() {
        discoveredLocalTools.clear();
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        
        for (String beanName : beanNames) {
            try {
                Object bean = applicationContext.getBean(beanName);
                if (beanName.contains("org.springframework")) continue;

                Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());
                for (Method method : methods) {
                    if (method.isAnnotationPresent(Tool.class)) {
                        // Spring AI 2.0.0-M5: Using builder and ToolDefinitions
                        ToolCallback callback = MethodToolCallback.builder()
                                .toolDefinition(ToolDefinitions.from(method))
                                .toolMethod(method)
                                .toolObject(bean)
                                .build();
                        discoveredLocalTools.add(callback);
                        log.debug("Discovered local tool: {} from bean: {}", callback.getToolDefinition().name(), beanName);
                    }
                }
            } catch (Exception e) {
                // Skip beans that cannot be accessed or are not relevant
            }
        }
        log.info("Local tool discovery finished. Found {} tools.", discoveredLocalTools.size());
    }

    /**
     * Periodically refreshes the tool cache (e.g., every 10 minutes).
     * This is useful for picking up dynamic changes from MCP servers or other providers.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void refreshTools() {
        log.debug("Refreshing tool cache. Providers: {}, Local discovered: {}", providers.size(), discoveredLocalTools.size());
        List<ToolCallback> allTools = new ArrayList<>();
        
        // 1. Always prioritize local discovered tools
        allTools.addAll(discoveredLocalTools);

        // 2. Fetch tools from all available providers (MCP, etc.)
        for (ToolCallbackProvider provider : providers) {
            try {
                ToolCallback[] callbacks = provider.getToolCallbacks();
                if (callbacks != null) {
                    allTools.addAll(List.of(callbacks));
                    log.debug("Loaded {} tools from provider: {}", callbacks.length, provider.getClass().getSimpleName());
                }
            } catch (Exception e) {
                // Fail-safe: If a provider (like MCP) fails, continue with others
                log.warn("Could not load tools from provider {}: {}. Skipping.", provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        // Atomic update of the cache
        cachedTools.set(Collections.unmodifiableList(allTools));
        log.info("Tool cache updated. Total tools available: {}", allTools.size());
        
        if (log.isDebugEnabled()) {
            allTools.forEach(t -> log.debug(" - Available Tool: {}", t.getToolDefinition().name()));
        }
    }

    /**
     * Retrieves all tools from the cache.
     * 
     * @return A list of {@link ToolCallback}s ready for use with {@link ChatClient}.
     */
    public List<ToolCallback> getAllTools() {
        List<ToolCallback> tools = cachedTools.get();
        if (tools.isEmpty()) {
            log.warn("Tool cache is empty! Attempting emergency refresh.");
            refreshTools();
            return cachedTools.get();
        }
        return tools;
    }

    /**
     * Returns the names of all currently available tools.
     */
    public List<String> getAllToolNames() {
        return getAllTools().stream()
                .map(t -> t.getToolDefinition().name())
                .collect(Collectors.toList());
    }
}
