package com.acaivis.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "mercadopago_order_id", nullable = false, unique = true, length = 100)
    private String mercadoPagoOrderId;

    @Column(name = "mercadopago_payment_id", length = 100)
    private String mercadoPagoPaymentId;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "status_detail", length = 100)
    private String statusDetail;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "order_request_json", nullable = false, columnDefinition = "TEXT")
    private String orderRequestJson;

    @Column(name = "resolved_order_id")
    private Long resolvedOrderId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getMercadoPagoOrderId() { return mercadoPagoOrderId; }
    public void setMercadoPagoOrderId(String value) { mercadoPagoOrderId = value; }
    public String getMercadoPagoPaymentId() { return mercadoPagoPaymentId; }
    public void setMercadoPagoPaymentId(String value) { mercadoPagoPaymentId = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getStatusDetail() { return statusDetail; }
    public void setStatusDetail(String value) { statusDetail = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public String getOrderRequestJson() { return orderRequestJson; }
    public void setOrderRequestJson(String value) { orderRequestJson = value; }
    public Long getResolvedOrderId() { return resolvedOrderId; }
    public void setResolvedOrderId(Long value) { resolvedOrderId = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
