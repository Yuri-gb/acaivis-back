CREATE TABLE order_email_notifications (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    event VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    CONSTRAINT uk_order_email_notification_event UNIQUE (order_id, event)
);

CREATE INDEX idx_order_email_notifications_order ON order_email_notifications(order_id);
CREATE INDEX idx_order_email_notifications_status ON order_email_notifications(status);
