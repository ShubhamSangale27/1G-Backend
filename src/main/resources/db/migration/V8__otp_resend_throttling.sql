ALTER TABLE visit_otps
    ADD COLUMN resend_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN first_sent_at TIMESTAMP,
    ADD COLUMN last_sent_at TIMESTAMP;

UPDATE visit_otps
SET first_sent_at = COALESCE(first_sent_at, created_at),
    last_sent_at = COALESCE(last_sent_at, created_at);

ALTER TABLE visit_otps
    ALTER COLUMN first_sent_at SET NOT NULL,
    ALTER COLUMN last_sent_at SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_otp_verifications_identifier_channel_created
    ON otp_verifications(identifier, channel, created_at);
