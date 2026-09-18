package com.acaivis.dto; import java.math.BigDecimal; public record OrderItemResponse(Long productId,String productName,String size,BigDecimal unitPrice,Integer quantity,BigDecimal subtotal) {}
