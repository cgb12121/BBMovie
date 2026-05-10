package bbmovie.ai_platform.aop_policy.hitl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresApproval {
    String action() default "tool_execution";
    String description() default "Requires explicit human approval before execution";
}
