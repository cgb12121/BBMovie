-- v4:
alter table users
    add is_mfa_enabled BIT(1) null;

alter table users
    add totp_secret VARCHAR(255) null;

alter table jwk_keys
    modify version BIGINT not null;

alter table refresh_token
    modify version BIGINT not null;

alter table users
    modify version BIGINT not null;