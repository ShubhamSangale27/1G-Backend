CREATE TABLE IF NOT EXISTS market_areas (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES market_areas(id) ON DELETE CASCADE,
    level VARCHAR(20) NOT NULL,
    name VARCHAR(150) NOT NULL,
    state_name VARCHAR(150),
    city_name VARCHAR(150),
    state_slug VARCHAR(160),
    city_slug VARCHAR(160),
    location_slug VARCHAR(160),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_market_areas_parent ON market_areas(parent_id);
CREATE INDEX IF NOT EXISTS idx_market_areas_level_active ON market_areas(level, active, sort_order);
CREATE INDEX IF NOT EXISTS idx_market_areas_state_city ON market_areas(state_name, city_name);
CREATE INDEX IF NOT EXISTS idx_market_areas_name_level ON market_areas(LOWER(name), level);

CREATE TABLE IF NOT EXISTS market_stat_snapshots (
    id BIGSERIAL PRIMARY KEY,
    market_area_id BIGINT NOT NULL REFERENCES market_areas(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,
    granularity VARCHAR(20) NOT NULL DEFAULT 'QUARTERLY',
    price_index DECIMAL(14, 4) NOT NULL,
    avg_price_per_sqft DECIMAL(14, 2),
    yoy_growth_pct DECIMAL(8, 4),
    transaction_volume INT,
    rental_yield_pct DECIMAL(8, 4),
    source_type VARCHAR(30) NOT NULL DEFAULT 'ADMIN',
    source_label VARCHAR(120),
    confidence_score DECIMAL(5, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_market_snapshots_area_date
    ON market_stat_snapshots(market_area_id, snapshot_date DESC);
CREATE UNIQUE INDEX IF NOT EXISTS uq_market_snapshots_area_date_granularity
    ON market_stat_snapshots(market_area_id, snapshot_date, granularity);

CREATE TABLE IF NOT EXISTS market_data_refresh_runs (
    id BIGSERIAL PRIMARY KEY,
    source_name VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    records_upserted INT NOT NULL DEFAULT 0,
    message TEXT,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP
);
