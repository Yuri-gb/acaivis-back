package com.acaivis.dto;
import jakarta.validation.constraints.*; import java.math.BigDecimal;
public record ProductRequest(@NotBlank String name,@NotBlank String size,@NotBlank @Size(max=500) String description,@NotNull @DecimalMin("0.00") BigDecimal price,@Size(max=500) String imageUrl,@Size(max=60) String badge,@NotNull @Min(0) Integer stockQuantity,@NotNull Boolean available,@NotNull Long categoryId) {}
