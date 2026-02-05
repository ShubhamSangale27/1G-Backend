-- Premium plans
INSERT INTO premium_plans (name, description, price, duration_days, stripe_price_id) VALUES
('Basic', 'Basic featured listing for 30 days', 999.00, 30, NULL),
('Premium', 'Premium featured listing for 90 days', 2499.00, 90, NULL),
('Annual', 'Featured listing for 1 year', 7999.00, 365, NULL);
