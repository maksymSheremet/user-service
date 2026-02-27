CREATE TABLE users
(
    id                   BIGINT PRIMARY KEY,
    email                VARCHAR(255) NOT NULL UNIQUE,
    full_name            VARCHAR(255),
    avatar_url           TEXT,
    timezone             VARCHAR(64)  NOT NULL DEFAULT 'UTC',
    language             VARCHAR(16)  NOT NULL DEFAULT 'en',
    onboarding_completed BOOLEAN      NOT NULL DEFAULT FALSE,
    status               VARCHAR(32)  NOT NULL DEFAULT 'ACTIVE',
    created_at           TIMESTAMP WITH TIME ZONE,
    updated_at           TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED'))
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);