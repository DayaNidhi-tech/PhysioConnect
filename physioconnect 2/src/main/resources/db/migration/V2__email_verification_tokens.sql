-- Email verification tokens (single-use, time-limited)
CREATE TABLE email_verification_tokens (
    token_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token       VARCHAR(64) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evt_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT uk_evt_token UNIQUE (token)
);

CREATE INDEX idx_evt_user ON email_verification_tokens (user_id);
