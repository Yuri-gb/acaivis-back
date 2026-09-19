package com.acaivis.dto.payment;

import com.acaivis.dto.OrderRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record MercadoPagoCardCheckoutRequest(
        @Valid
        @NotNull
        OrderRequest order,

        @Valid
        @NotNull
        MercadoPagoPaymentRequest payment
) {
}
