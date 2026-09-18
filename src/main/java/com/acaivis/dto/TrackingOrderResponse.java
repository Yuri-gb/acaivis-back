package com.acaivis.dto;
import com.acaivis.model.OrderStatus; import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.List;
public record TrackingOrderResponse(String trackingCode,OrderStatus status,LocalDateTime createdAt,BigDecimal subtotal,BigDecimal deliveryFee,BigDecimal total,String deliveryZoneName,String address,List<OrderItemResponse> items) {}
