package com.example.bbmovie.entity.enumerate;

import lombok.Getter;

import java.util.Set;

//TODO: implement permission management for jwt and spring security
@Getter
public enum Role {
    USER(Set.of(
            Permission.MOVIE_READ,
            Permission.COMMENT_CREATE
    )),

    MODERATOR(Set.of(
            Permission.MOVIE_READ,
            Permission.COMMENT_CREATE,
            Permission.COMMENT_DELETE,
            Permission.COMMENT_MODERATE
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
            Permission.COMMENT_MODERATE,
            Permission.USER_READ,
            Permission.USER_BAN,
            Permission.ROLE_ASSIGN
    ));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
