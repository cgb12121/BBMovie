package com.example.bbmovie.security.expression;

import com.example.bbmovie.entity.Movie;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (targetDomainObject instanceof Movie movie && permission.equals("edit")) {
            String userId = auth.getName();
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        return checkFromDatabase(targetId, targetType, permission, auth.getName());
    }

    private boolean checkFromDatabase(Serializable targetId, String targetType, Object permission, String name) {
        return false;
    }
}
