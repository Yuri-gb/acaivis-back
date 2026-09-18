package com.acaivis.dto;
import com.acaivis.model.*; import java.math.BigDecimal; import java.time.LocalDateTime; import java.util.List;
public record DeliveryOrderResponse(Long id,String trackingCode,String customerName,String customerPhone,String address,String neighborhood,String city,String state,String zipCode,BigDecimal total,PaymentMethod paymentMethod,boolean paymentConfirmed,OrderStatus status,List<OrderItemResponse> items,Integer routeOrder,String deliveryProofUrl,LocalDateTime deliveredAt,String deliveredBy) {}
