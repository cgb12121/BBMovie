package com.bbmovie.ai_assistant_service.hitl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a tool method as requiring risk assessment and potential approval.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresApproval {
    /**
     * The base risk level of the action itself, regardless of arguments.
     */
    RiskLevel baseRisk() default RiskLevel.LOW;

    /**
     * The type of action being performed.
     */
    ActionType action();

    /**
     * Human-readable description of what this action does.
     */
    String description() default "";
}
