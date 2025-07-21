package com.bbmovie.auth.entity.enumerate;

import lombok.Getter;

@Getter
public enum Permission {
    // Movie actions
    MOVIE_READ("movie:read"),
    MOVIE_CREATE("movie:create"),
    MOVIE_UPDATE("movie:update"),
    MOVIE_DELETE("movie:delete"),

    // Comment actions
    COMMENT_CREATE("comment:create"),
    COMMENT_DELETE("comment:delete"),
    COMMENT_MODERATE("comment:moderate"),

    // User management
    USER_READ("user:read"),
    USER_BAN("user:ban"),

    // Role management
    ROLE_ASSIGN("role:assign");

    private final String value;

    Permission(String value) {
        this.value = value;
    }
}
