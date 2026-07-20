CREATE TABLE blog_posts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(300) NOT NULL UNIQUE,
    excerpt VARCHAR(1000),
    cover_image_url VARCHAR(1000),
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_blog_posts_published ON blog_posts(published);
CREATE INDEX idx_blog_posts_published_at ON blog_posts(published_at DESC);
CREATE INDEX idx_blog_posts_author ON blog_posts(author_id);

CREATE TABLE blog_content_blocks (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES blog_posts(id) ON DELETE CASCADE,
    block_type VARCHAR(20) NOT NULL,
    content TEXT,
    media_url VARCHAR(1000),
    link_url VARCHAR(1000),
    caption VARCHAR(500),
    display_order INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_blog_blocks_post ON blog_content_blocks(post_id);
CREATE INDEX idx_blog_blocks_order ON blog_content_blocks(post_id, display_order);

