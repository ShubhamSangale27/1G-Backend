CREATE TABLE site_visit_comments (
    id BIGSERIAL PRIMARY KEY,
    site_visit_id BIGINT NOT NULL REFERENCES site_visits(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    comment_text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_site_visit_comments_visit ON site_visit_comments(site_visit_id);
