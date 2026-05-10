package bbmovie.ai_platform.agentic_ai.service;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final AtomicReference<List<ToolCallback>> cachedTools = new AtomicReference<>(new ArrayList<>());

    /**
     * Tự động làm mới danh sách Tool khi ứng dụng khởi động xong.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Application ready. Initializing tool cache...");
        refreshTools();
    }

    /**
     * Định kỳ làm mới danh sách Tool (Ví dụ: mỗi 10 phút) 
     * để cập nhật các Tool mới từ MCP Server nếu có.
     */
    @Scheduled(fixedRate = 600000) // 10 minutes
    public void refreshTools() {
        log.debug("Refreshing tool cache from {} providers...", providers.size());
        List<ToolCallback> allTools = new ArrayList<>();
        
        for (ToolCallbackProvider provider : providers) {
            try {
                // Thử nhặt tool từ provider
                ToolCallback[] callbacks = provider.getToolCallbacks();
                if (callbacks != null) {
                    allTools.addAll(List.of(callbacks));
                    log.debug("Loaded {} tools from {}", callbacks.length, provider.getClass().getSimpleName());
                }
            } catch (Exception e) {
                // Fail-safe: Nếu một provider (như MCP) bị lỗi/ngắt kết nối, 
                // chúng ta chỉ log cảnh báo và tiếp tục với các provider khác.
                log.warn("Could not load tools from provider {}: {}. Skipping.", provider.getClass().getSimpleName(), e.getMessage());
            }
        }

        List<ToolCallback> oldTools = cachedTools.getAndSet(Collections.emptyList());
        if (oldTools != null) {
            oldTools.clear(); 
        }        
        cachedTools.set(allTools);
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
