package bbmovie.ai_platform.aop_policy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce ownership checks on resources.
 * Supports SpEL expressions to extract IDs from method arguments.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckOwnership {
    
    /**
     * SpEL expression to extract the resource ID (e.g., "#sessionId").
     */
    String expression();

    /**
     * The type of entity being checked (e.g., "SESSION", "MESSAGE").
     */
    String entityType() default "SESSION";
}
