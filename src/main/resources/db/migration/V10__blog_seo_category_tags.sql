ALTER TABLE blog_posts ADD COLUMN meta_title VARCHAR(255);
ALTER TABLE blog_posts ADD COLUMN meta_description VARCHAR(500);
ALTER TABLE blog_posts ADD COLUMN category VARCHAR(100);
ALTER TABLE blog_posts ADD COLUMN tags VARCHAR(500);

CREATE INDEX idx_blog_posts_category ON blog_posts(category);
