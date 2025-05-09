package com.example.bbmovie.security.expression;

import org.springframework.security.core.Authentication;

import java.io.Serializable;

public interface PermissionEvaluator extends org.springframework.security.access.PermissionEvaluator {
    boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission);
    boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission);
}
