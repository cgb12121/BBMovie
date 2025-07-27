package com.bbmovie.auth.entity.enumerate;

import lombok.Getter;

import java.util.Set;

@Getter
public enum Role {
    USER(Set.of(
            Permission.MOVIE_READ,
            Permission.COMMENT_CREATE,
            Permission.COMMENT_EDIT
    )),
    MODERATOR(Set.of(
            Permission.MOVIE_READ,
            Permission.COMMENT_CREATE,
            Permission.COMMENT_DELETE
    )),
    ADMIN(Set.of(
            Permission.MOVIE_READ,
            Permission.MOVIE_CREATE,
            Permission.MOVIE_UPDATE,
            Permission.MOVIE_DELETE,
            Permission.COMMENT_DELETE,
            Permission.USER_READ,
            Permission.USER_BAN
    )),
    SUPER_ADMIN(Set.of(
            Permission.MOVIE_READ,
            Permission.MOVIE_CREATE,
            Permission.MOVIE_UPDATE,
            Permission.MOVIE_DELETE,
            Permission.COMMENT_CREATE,
            Permission.COMMENT_DELETE,
            Permission.USER_READ,
            Permission.USER_BAN,
            Permission.ROLE_ASSIGN
    ));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}