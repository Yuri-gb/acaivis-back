CREATE TABLE payment_attempts (
    id VARCHAR(36) PRIMARY KEY,
    mercadopago_order_id VARCHAR(100) NOT NULL UNIQUE,
    mercadopago_payment_id VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    status_detail VARCHAR(100),
    amount NUMERIC(10,2) NOT NULL,
    order_request_json TEXT NOT NULL,
    resolved_order_id BIGINT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_payment_attempts_payment_id
    ON payment_attempts(mercadopago_payment_id);

CREATE INDEX idx_payment_attempts_resolved_order
    ON payment_attempts(resolved_order_id);
