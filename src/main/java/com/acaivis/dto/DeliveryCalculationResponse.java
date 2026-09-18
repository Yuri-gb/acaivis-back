package com.acaivis.dto;

import java.math.BigDecimal;

public record DeliveryCalculationResponse(
        String zipCode,
        String street,
        String neighborhood,
        String city,
        String state,
        String ibge,
        Long deliveryZoneId,
        String deliveryZoneName,
        BigDecimal deliveryFee
) {}
