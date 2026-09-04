CREATE TABLE refresh_tokens (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_refresh_tokens_token
        UNIQUE (token),

    CONSTRAINT fk_refresh_token_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id),

    INDEX idx_refresh_token_token (token),
    INDEX idx_refresh_token_user_id (user_id)
);
