package com.acaivis.dto;
import com.acaivis.model.OrderStatus; import java.time.LocalDateTime;
public record OrderStatusHistoryResponse(OrderStatus status,LocalDateTime createdAt) {}
