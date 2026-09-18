package com.acaivis.dto; import jakarta.validation.constraints.*; import java.math.BigDecimal; public record DeliveryZoneRequest(@NotBlank String name,@NotNull @DecimalMin("0.00") BigDecimal fee) {}
