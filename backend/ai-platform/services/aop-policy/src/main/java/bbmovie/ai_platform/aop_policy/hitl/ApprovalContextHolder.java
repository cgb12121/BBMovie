package bbmovie.ai_platform.aop_policy.hitl;

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
