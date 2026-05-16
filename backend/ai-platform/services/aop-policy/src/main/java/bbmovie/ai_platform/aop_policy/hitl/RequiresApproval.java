package bbmovie.ai_platform.aop_policy.hitl;

import bbmovie.ai_platform.aop_policy.hitl.RiskLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresApproval {
    
    String action() default "tool_execution";

    String description() default "Requires explicit human approval before execution";

    /** Risk level of this tool. Triggers HITL if >= MEDIUM. */
    RiskLevel riskLevel() default RiskLevel.MEDIUM;

    /**
     * Roles that are permitted to trigger this tool.
     * Empty = no role restriction (any authenticated user).
     * Example: {"ADMIN"} means only ADMIN role can invoke.
     */
    String[] requiredRoles() default {};
}
