package bbmovie.ai_platform.agentic_ai.service;

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
 * Quản lý và tổng hợp các Tool từ nhiều nguồn khác nhau:
 * 1. Các Tool nội bộ (Annotated with @Tool)
 * 2. Các Tool từ MCP (Model Context Protocol)
 * 
 * Có cơ chế Cache để tránh gọi sang MCP Server quá nhiều lần.
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
     * Tự động làm mới danh sách Tool khi ứng dụng khởi động xong.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready. Discovering local tools and initializing tool cache...");
        discoverLocalTools();
        refreshTools();
    }

    /**
     * Quét tất cả các bean trong context để tìm các phương thức có đánh dấu @Tool.
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
                        // Spring AI 2.0.0-M5: Sử dụng builder và ToolDefinitions
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
                // Skip
            }
        }
        log.info("Local tool discovery finished. Found {} tools.", discoveredLocalTools.size());
    }

    /**
     * Định kỳ làm mới danh sách Tool (Ví dụ: mỗi 10 phút) 
     * để cập nhật các Tool mới từ MCP Server nếu có.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void refreshTools() {
        log.debug("Refreshing tool cache. Providers: {}, Local discovered: {}", providers.size(), discoveredLocalTools.size());
        List<ToolCallback> allTools = new ArrayList<>();
        
        // 1. Luôn ưu tiên nạp các tool nội bộ đã khám phá được
        allTools.addAll(discoveredLocalTools);

        // 2. Thử nạp thêm tools từ các provider (MCP, etc.)
        for (ToolCallbackProvider provider : providers) {
            try {
                // Thử nhặt tool từ provider
                ToolCallback[] callbacks = provider.getToolCallbacks();
                if (callbacks != null) {
                    allTools.addAll(List.of(callbacks));
                    log.debug("Loaded {} tools from provider: {}", callbacks.length, provider.getClass().getSimpleName());
                }
            } catch (Exception e) {
                // Fail-safe: Nếu một provider (như MCP) bị lỗi/ngắt kết nối, 
                // chúng ta chỉ log cảnh báo và tiếp tục với các provider khác.
                log.warn("Could not load tools from provider {}: {}. Skipping.", provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        // Cập nhật cache nguyên tử
        cachedTools.set(Collections.unmodifiableList(allTools));
        log.info("Tool cache updated. Total tools available: {}", allTools.size());
        
        if (log.isDebugEnabled()) {
            allTools.forEach(t -> log.debug(" - Available Tool: {}", t.getToolDefinition().name()));
        }
    }

    /**
     * Lấy danh sách Tool từ Cache (Cực nhanh).
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
     * Lấy tên của tất cả các Tool trong Cache.
     */
    public List<String> getAllToolNames() {
        return getAllTools().stream()
                .map(t -> t.getToolDefinition().name())
                .collect(Collectors.toList());
    }
}
