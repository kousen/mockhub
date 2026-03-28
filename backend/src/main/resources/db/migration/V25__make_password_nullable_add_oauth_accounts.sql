-- Allow OAuth-only users who have no password
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

-- Track linked OAuth provider accounts per user
CREATE TABLE oauth_accounts (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_account_id VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_oauth_provider_account UNIQUE (provider, provider_account_id)
);

CREATE INDEX idx_oauth_accounts_user_id ON oauth_accounts (user_id);
