package com.acaivis.dto;

import com.acaivis.model.OrderStatus;
import com.acaivis.model.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TrackingOrderResponse(
        String trackingCode,
        OrderStatus status,
        LocalDateTime createdAt,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal total,
        String deliveryZoneName,
        String address,
        PaymentMethod paymentMethod,
        boolean paymentConfirmed,
        List<OrderItemResponse> items
) {}
