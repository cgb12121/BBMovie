package com.bbmovie.ai_assistant_service.hitl;

/**
 * Holds ExecutionContext for the current thread.
 * Required because AOP intercepts method calls where we might not have
 * easy access to the WebFlux/Reactor context or arguments.
 * <p>
 * Usage: ToolExecutor sets this before invoking the tool and clears it after.
 */
public class ApprovalContextHolder {
    private static final ThreadLocal<ExecutionContext> CONTEXT = new ThreadLocal<>();

    public static void set(ExecutionContext context) {
        CONTEXT.set(context);
    }

    public static ExecutionContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
