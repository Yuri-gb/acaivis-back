package com.acaivis.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_code", nullable = false, unique = true, updatable = false, length = 20)
    private String trackingCode;

    @Column(name = "comanda_number", nullable = false, unique = true, updatable = false)
    private Long comandaNumber;

    @Column(name = "customer_name", nullable = false)
    private String customerName;

    @Column(name = "customer_phone", nullable = false)
    private String customerPhone;

    @Column(name = "customer_email")
    private String customerEmail;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String number;

    private String complement;

    @Column(nullable = false)
    private String neighborhood;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false, length = 2)
    private String state;

    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_zone_id", nullable = false)
    private DeliveryZone deliveryZone;

    @Column(name = "delivery_zone_name", nullable = false, updatable = false)
    private String deliveryZoneName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_confirmed", nullable = false)
    private boolean paymentConfirmed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    // Mercado Pago
    @Column(name = "mercadopago_order_id", length = 100)
    private String mercadoPagoOrderId;

    @Column(name = "delivery_route_order")
    private Integer deliveryRouteOrder;

    @Column(name = "delivery_proof_url", length = 1000)
    private String deliveryProofUrl;

    @Column(name = "delivery_proof_path", length = 500)
    private String deliveryProofPath;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "delivered_by", length = 255)
    private String deliveredBy;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "order",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<OrderItem> items = new ArrayList<>();

    @PrePersist
    void pre() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void upd() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String v) {
        trackingCode = v;
    }

    public Long getComandaNumber() {
        return comandaNumber;
    }

    public void setComandaNumber(Long v) {
        comandaNumber = v;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String v) {
        customerName = v;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String v) {
        customerPhone = v;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String v) {
        customerEmail = v;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String v) {
        street = v;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String v) {
        number = v;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String v) {
        complement = v;
    }

    public String getNeighborhood() {
        return neighborhood;
    }

    public void setNeighborhood(String v) {
        neighborhood = v;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String v) {
        city = v;
    }

    public String getState() {
        return state;
    }

    public void setState(String v) {
        state = v;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String v) {
        zipCode = v;
    }

    public DeliveryZone getDeliveryZone() {
        return deliveryZone;
    }

    public String getDeliveryZoneName() {
        return deliveryZoneName;
    }

    public void setDeliveryZoneName(String v) {
        deliveryZoneName = v;
    }

    public void setDeliveryZone(DeliveryZone v) {
        deliveryZone = v;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal v) {
        subtotal = v;
    }

    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    public void setDeliveryFee(BigDecimal v) {
        deliveryFee = v;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal v) {
        discount = v;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal v) {
        total = v;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public boolean isPaymentConfirmed() {
        return paymentConfirmed;
    }

    public void setPaymentConfirmed(boolean v) {
        paymentConfirmed = v;
    }

    public void setPaymentMethod(PaymentMethod v) {
        paymentMethod = v;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus v) {
        status = v;
    }

    public String getMercadoPagoOrderId() {
        return mercadoPagoOrderId;
    }

    public void setMercadoPagoOrderId(String v) {
        mercadoPagoOrderId = v;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String v) {
        notes = v;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Integer getDeliveryRouteOrder() { return deliveryRouteOrder; }
    public void setDeliveryRouteOrder(Integer v) { deliveryRouteOrder = v; }
    public String getDeliveryProofUrl() { return deliveryProofUrl; }
    public void setDeliveryProofUrl(String v) { deliveryProofUrl = v; }
    public String getDeliveryProofPath() { return deliveryProofPath; }
    public void setDeliveryProofPath(String v) { deliveryProofPath = v; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime v) { deliveredAt = v; }
    public String getDeliveredBy() { return deliveredBy; }
    public void setDeliveredBy(String v) { deliveredBy = v; }

    public List<OrderItem> getItems() {
        return items;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}