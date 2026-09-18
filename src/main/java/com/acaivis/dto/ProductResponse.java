package com.acaivis.dto;
import java.math.BigDecimal; import java.time.LocalDateTime;
public record ProductResponse(Long id,String name,String size,String description,BigDecimal price,String imageUrl,String badge,Integer stockQuantity,boolean available,Long categoryId,String categoryName,LocalDateTime createdAt,LocalDateTime updatedAt) {}
