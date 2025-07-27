alter table users
    add age INT null;

alter table users
    add parental_controls_enabled BIT(1) null;

alter table users
    add phone_number VARCHAR(255) null;

alter table users
    add region VARCHAR(255) null;

alter table users
    add subscription_tier VARCHAR(255) null;

alter table refresh_token
    add constraint uc_refresh_token_sid unique (sid);

alter table users
    add constraint uc_users_phone_number unique (phone_number);

alter table refresh_token
    drop column sid;

alter table refresh_token
    add sid VARCHAR(255) not null;

alter table refresh_token
    add constraint uc_refresh_token_sid unique (sid);

alter table jwk_keys
    modify version BIGINT not null;

alter table refresh_token
    modify version BIGINT not null;

alter table users
    modify version BIGINT not null;