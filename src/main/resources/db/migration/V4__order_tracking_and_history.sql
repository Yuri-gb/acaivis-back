ALTER TABLE orders ADD COLUMN tracking_code VARCHAR(20);
ALTER TABLE orders ADD COLUMN comanda_number BIGINT;
ALTER TABLE orders ADD COLUMN customer_email VARCHAR(180);
ALTER TABLE orders ADD COLUMN notes VARCHAR(1000);
ALTER TABLE orders ADD COLUMN payment_confirmed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN delivery_zone_name VARCHAR(100);

CREATE SEQUENCE order_comanda_seq START WITH 1;

UPDATE orders SET tracking_code = 'AC-' || UPPER(SUBSTRING(MD5(RANDOM()::TEXT || id::TEXT) FROM 1 FOR 6)) WHERE tracking_code IS NULL;
UPDATE orders SET comanda_number = id WHERE comanda_number IS NULL;

SELECT setval('order_comanda_seq', COALESCE((SELECT MAX(comanda_number) FROM orders), 0) + 1, false);

UPDATE orders SET delivery_zone_name = (SELECT name FROM delivery_zones z WHERE z.id = orders.delivery_zone_id);
UPDATE orders SET payment_confirmed = CASE WHEN status IN ('PAID','PREPARING','READY','OUT_FOR_DELIVERY','DELIVERED') THEN TRUE ELSE FALSE END;

ALTER TABLE orders ALTER COLUMN delivery_zone_name SET NOT NULL;

ALTER TABLE orders ALTER COLUMN tracking_code SET NOT NULL;
ALTER TABLE orders ALTER COLUMN comanda_number SET NOT NULL;
ALTER TABLE orders ADD CONSTRAINT uq_orders_tracking_code UNIQUE (tracking_code);
ALTER TABLE orders ADD CONSTRAINT uq_orders_comanda_number UNIQUE (comanda_number);
CREATE INDEX idx_orders_customer_phone ON orders(customer_phone);
CREATE INDEX idx_orders_tracking_code ON orders(tracking_code);

CREATE TABLE order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_order_status_history_order ON order_status_history(order_id, created_at);

INSERT INTO order_status_history (order_id, status, created_at)
SELECT id, status, created_at FROM orders;
