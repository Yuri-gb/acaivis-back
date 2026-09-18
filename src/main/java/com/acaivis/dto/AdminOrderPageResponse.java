package com.acaivis.dto;

import java.util.List;

public record AdminOrderPageResponse(
        List<OrderResponse> content,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize
) {}
