package com.bbmovie.ai_assistant_service.hitl;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in a DTO as containing sensitive data.
 * If this field is modified (or present in a request), it contributes to the risk score.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveData {
    /**
     * The risk level associated with modifying or exposing this data.
     */
    RiskLevel level() default RiskLevel.MEDIUM;
}
