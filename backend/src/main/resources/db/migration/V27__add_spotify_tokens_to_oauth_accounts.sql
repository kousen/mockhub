ALTER TABLE oauth_accounts ADD COLUMN access_token_encrypted TEXT;
ALTER TABLE oauth_accounts ADD COLUMN refresh_token_encrypted TEXT;
ALTER TABLE oauth_accounts ADD COLUMN token_expires_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE oauth_accounts ADD COLUMN scopes_granted VARCHAR(500);
