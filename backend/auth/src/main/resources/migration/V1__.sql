create table jwk_keys
(
    id                 BINARY(16)    not null,
    created_date       datetime      not null,
    last_modified_date datetime      not null,
    version            BIGINT        null,
    kid                VARCHAR(255)  null,
    public_jwk         VARCHAR(4096) not null,
    private_jwk        VARCHAR(4096) null,
    is_active          BIT(1)        not null,
    created_at         datetime      null,
    constraint pk_jwk_keys primary key (id)
);

create table refresh_token
(
    id                 BINARY(16)    not null,
    created_date       datetime      not null,
    last_modified_date datetime      not null,
    version            BIGINT        null,
    email              VARCHAR(255)  not null,
    device_name        VARCHAR(255)  not null,
    device_os          VARCHAR(255)  not null,
    device_ip_address  VARCHAR(255)  not null,
    browser            VARCHAR(255)  not null,
    browser_version    VARCHAR(255)  not null,
    token              VARCHAR(1000) not null,
    jti                VARCHAR(255)  not null,
    sid                BINARY(16)    not null,
    expiry_date        datetime      not null,
    revoked            BIT(1)        not null,
    constraint pk_refresh_token primary key (id)
);

create table users
(
    id                         BINARY(16)   not null,
    created_date               datetime     not null,
    last_modified_date         datetime     not null,
    version                    BIGINT       null,
    displayed_username         VARCHAR(255) not null,
    password                   VARCHAR(255) not null,
    email                      VARCHAR(255) not null,
    auth_provider              VARCHAR(255) null,
    first_name                 VARCHAR(255) null,
    last_name                  VARCHAR(255) null,
    profile_picture_url        VARCHAR(255) null,
    is_enabled                 BIT(1)       null,
    is_account_non_expired     BIT(1)       null,
    is_account_non_locked      BIT(1)       null,
    is_credentials_non_expired BIT(1)       null,
    last_logged_in             datetime     null,
    `role`                     VARCHAR(255) not null,
    constraint pk_users primary key (id)
);

alter table users
    add constraint uc_users_displayedusername unique (displayed_username);

alter table users
    add constraint uc_users_email unique (email);