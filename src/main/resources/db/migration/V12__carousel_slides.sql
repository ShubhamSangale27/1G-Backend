CREATE TABLE carousel_slides (
    id BIGSERIAL PRIMARY KEY,
    image_url VARCHAR(1000) NOT NULL,
    link_url VARCHAR(1000),
    alt_text VARCHAR(255),
    display_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_carousel_slides_active_order ON carousel_slides (active, display_order);
