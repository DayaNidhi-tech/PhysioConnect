CREATE TABLE jwt_revocations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    jti VARCHAR(255) NOT NULL,
    expires_at DATETIME NOT NULL,
    revoked_at DATETIME NOT NULL,

    PRIMARY KEY (id),

    CONSTRAINT uk_jwt_revocations_jti
        UNIQUE (jti),

    INDEX idx_jwt_revocations_expires_at (expires_at)
);