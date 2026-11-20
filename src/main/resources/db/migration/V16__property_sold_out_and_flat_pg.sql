-- Sold-out flag for admin-marked properties (e-commerce style overlay)
ALTER TABLE properties
    ADD COLUMN IF NOT EXISTS sold_out BOOLEAN NOT NULL DEFAULT FALSE;

-- Rename APARTMENT → FLAT and allow PG as a property type (string enum column)
UPDATE properties
SET property_type = 'FLAT'
WHERE property_type = 'APARTMENT';

CREATE INDEX IF NOT EXISTS idx_properties_sold_out ON properties (sold_out);
