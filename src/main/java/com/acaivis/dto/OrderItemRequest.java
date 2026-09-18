package com.acaivis.dto; import jakarta.validation.constraints.*; public record OrderItemRequest(@NotNull Long productId,@NotNull @Min(1) Integer quantity) {}
