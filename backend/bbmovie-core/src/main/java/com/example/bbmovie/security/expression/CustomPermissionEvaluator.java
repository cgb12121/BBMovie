package com.example.bbmovie.security.expression;

import com.example.bbmovie.entity.Movie;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    //TODO: implement properly
    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        if (targetDomainObject instanceof Movie movie && permission.equals("edit")) {
            String userId = auth.getName();
        }
        return false;
    }

    @Override
    //TODO: implement properly
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        return checkFromDatabase(targetId, targetType, permission, auth.getName());
    }

    //TODO: implement properly
    private boolean checkFromDatabase(Serializable targetId, String targetType, Object permission, String name) {
        return false;
    }
}
