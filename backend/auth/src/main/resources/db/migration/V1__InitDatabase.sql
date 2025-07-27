-- Users table
CREATE TABLE users (
    id BINARY(16) NOT NULL,
    created_date DATETIME NOT NULL,
    last_modified_date DATETIME NOT NULL,
    version BIGINT NOT NULL,
    displayed_username VARCHAR(255) NOT NULL,
    password VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(255),
    auth_provider VARCHAR(255),
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    profile_picture_url VARCHAR(255),
    age INT,
    region VARCHAR(255),
    subscription_tier VARCHAR(255),
    parental_controls_enabled BIT(1) DEFAULT 0,
    role VARCHAR(255) NOT NULL,
    is_enabled BIT(1) DEFAULT 0,
    is_account_non_expired BIT(1) DEFAULT 1,
    is_account_non_locked BIT(1) DEFAULT 1,
    is_credentials_non_expired BIT(1) DEFAULT 1,
    last_login_time DATETIME,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uc_users_email UNIQUE (email),
    CONSTRAINT uc_users_phone_number UNIQUE (phone_number),
    CONSTRAINT uc_users_displayed_username UNIQUE (displayed_username)
);

-- Jwk_keys table
CREATE TABLE jwk_keys (
    id BINARY(16) NOT NULL,
    created_date DATETIME NOT NULL,
    last_modified_date DATETIME NOT NULL,
    version BIGINT NOT NULL,
    kid VARCHAR(255),
    public_jwk VARCHAR(4096) NOT NULL,
    private_jwk VARCHAR(4096),
    is_active BIT(1) NOT NULL,
    CONSTRAINT pk_jwk_keys PRIMARY KEY (id)
);

-- Refresh_token table
CREATE TABLE refresh_token (
    id BINARY(16) NOT NULL,
    created_date DATETIME NOT NULL,
    last_modified_date DATETIME NOT NULL,
    version BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    device_name VARCHAR(255) NOT NULL,
    device_os VARCHAR(255) NOT NULL,
    device_ip_address VARCHAR(255) NOT NULL,
    browser VARCHAR(255) NOT NULL,
    browser_version VARCHAR(255) NOT NULL,
    token VARCHAR(1000) NOT NULL,
    jti VARCHAR(255) NOT NULL,
    sid VARCHAR(255) NOT NULL,
    expiry_date DATETIME NOT NULL,
    revoked BIT(1) NOT NULL,
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uc_refresh_token_sid UNIQUE (sid)
);