CREATE TABLE device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    fcm_token VARCHAR(512) NOT NULL,
    platform VARCHAR(16) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_tokens_fcm_token UNIQUE (fcm_token)
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);
CREATE INDEX idx_device_tokens_active ON device_tokens(active);

CREATE TABLE push_campaigns (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(2000) NOT NULL,
    image_url VARCHAR(2048),
    link_url VARCHAR(2048),
    link_target VARCHAR(16) NOT NULL DEFAULT 'APP',
    target_role VARCHAR(16) NOT NULL DEFAULT 'ALL',
    sent_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    sent_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_push_campaigns_created_at ON push_campaigns(created_at DESC);
