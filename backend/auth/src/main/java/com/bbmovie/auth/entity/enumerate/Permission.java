package com.bbmovie.auth.entity.enumerate;

import lombok.Getter;

@Getter
public enum Permission {
    MOVIE_READ("movie:read"),
    MOVIE_CREATE("movie:create"),
    MOVIE_UPDATE("movie:update"),
    MOVIE_DELETE("movie:delete"),

    COMMENT_CREATE("comment:create"),
    COMMENT_DELETE("comment:delete"),
    COMMENT_EDIT("comment:edit"),

    USER_READ("user:read"),
    USER_BAN("user:ban"),
    ROLE_ASSIGN("role:assign");

    private final String value;

    Permission(String value) {
        this.value = value;
    }
}
