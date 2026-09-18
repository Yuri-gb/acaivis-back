package com.acaivis.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "order_email_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_email_notification_event",
                columnNames = {"order_id", "event"}
        )
)
public class OrderEmailNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, length = 50)
    private String event;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    protected OrderEmailNotification() {}

    public OrderEmailNotification(Order order, String event) {
        this.order = order;
        this.event = event;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public String getEvent() { return event; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getSentAt() { return sentAt; }

    public void markSent() {
        this.status = "SENT";
        this.sentAt = LocalDateTime.now();
    }
}
