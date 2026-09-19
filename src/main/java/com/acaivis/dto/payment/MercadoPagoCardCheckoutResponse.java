package com.acaivis.dto.payment;

import com.acaivis.dto.OrderResponse;

public record MercadoPagoCardCheckoutResponse(
        MercadoPagoPaymentResponse payment,
        OrderResponse order
) {
}
