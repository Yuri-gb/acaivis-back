package com.acaivis.service;

public enum OrderEmailEvent {
    PAYMENT_PENDING,
    PAYMENT_CONFIRMED,
    PREPARING,
    READY,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
