INSERT INTO categories (name) VALUES ('Açaís'), ('Combos') ON CONFLICT (name) DO NOTHING;
INSERT INTO delivery_zones (name, fee) VALUES ('Centro', 8.00), ('Tomba', 10.00), ('Brasília', 12.00) ON CONFLICT (name) DO NOTHING;
