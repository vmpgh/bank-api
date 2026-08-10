CREATE TABLE users
(
    id UUID PRIMARY KEY,

    username VARCHAR(100) NOT NULL UNIQUE,

    email VARCHAR(255) NOT NULL UNIQUE,

    password VARCHAR(255) NOT NULL,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100) NOT NULL,

    role VARCHAR(50) NOT NULL,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE accounts
(
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    balance NUMERIC(19,2) NOT NULL,

    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    deleted_at TIMESTAMP WITH TIME ZONE,

    version BIGINT NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
);

CREATE INDEX idx_accounts_user_id
    ON accounts(user_id);