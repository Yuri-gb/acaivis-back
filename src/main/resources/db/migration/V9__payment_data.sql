ALTER TABLE orders
    ADD COLUMN mercadopago_payment_id VARCHAR(100),
    ADD COLUMN payment_status VARCHAR(50),
    ADD COLUMN payment_status_detail VARCHAR(100),
    ADD COLUMN payment_expires_at TIMESTAMP,
    ADD COLUMN pix_qr_code VARCHAR(2000),
    ADD COLUMN pix_qr_code_base64 TEXT,
    ADD COLUMN pix_ticket_url VARCHAR(2000),
    ADD COLUMN stock_released BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_orders_mercadopago_order_id ON orders(mercadopago_order_id);
CREATE INDEX idx_orders_mercadopago_payment_id ON orders(mercadopago_payment_id);
