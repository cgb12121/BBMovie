alter table refresh_token
    add constraint uc_refresh_token_sid unique (sid);

alter table refresh_token
    drop column sid;

alter table refresh_token
    add sid VARCHAR(255) not null;

alter table refresh_token
    add constraint uc_refresh_token_sid unique (sid);