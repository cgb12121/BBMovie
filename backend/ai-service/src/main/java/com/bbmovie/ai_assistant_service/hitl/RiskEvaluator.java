package com.bbmovie.ai_assistant_service.hitl;

import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Component
public class RiskEvaluator {

    public RiskLevel evaluate(Method method, Object[] args) {
        RequiresApproval annotation = method.getAnnotation(RequiresApproval.class);
        RiskLevel maxRisk = (annotation != null) ? annotation.baseRisk() : RiskLevel.NONE;

        if (args != null) {
            for (Object arg : args) {
                RiskLevel argRisk = inspectObject(arg);
                if (argRisk.ordinal() > maxRisk.ordinal()) {
                    maxRisk = argRisk;
                }
            }
        }
        return maxRisk;
    }

    private RiskLevel inspectObject(Object obj) {
        if (obj == null) return RiskLevel.NONE;
        // Basic type checks (String, Integer) -> NONE
        if (isPrimitiveOrWrapper(obj.getClass())) return RiskLevel.NONE;

        RiskLevel currentRisk = RiskLevel.NONE;
        Class<?> clazz = obj.getClass();

        // Scan fields
        for (Field field : clazz.getDeclaredFields()) {
            SensitiveData sensitive = field.getAnnotation(SensitiveData.class);
            if (sensitive != null) {
                field.setAccessible(true);
                try {
                    Object val = field.get(obj);
                    // Only risky if the value is present/changed
                    if (val != null) {
                        if (sensitive.level().ordinal() > currentRisk.ordinal()) {
                            currentRisk = sensitive.level();
                        }
                    }
                } catch (IllegalAccessException e) { /* ignore */ }
            }
        }
        return currentRisk;
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                Number.class.isAssignableFrom(clazz) ||
                Boolean.class == clazz;
    }
}
