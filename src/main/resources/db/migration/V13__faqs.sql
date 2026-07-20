CREATE TABLE IF NOT EXISTS faqs (
    id BIGSERIAL PRIMARY KEY,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    keywords VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_faqs_active_sort ON faqs(active, sort_order);

CREATE TABLE IF NOT EXISTS unmatched_faq_questions (
    id BIGSERIAL PRIMARY KEY,
    question_text TEXT NOT NULL,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    promoted_faq_id BIGINT REFERENCES faqs(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_unmatched_faq_status ON unmatched_faq_questions(status);
CREATE INDEX IF NOT EXISTS idx_unmatched_faq_created ON unmatched_faq_questions(created_at DESC);
