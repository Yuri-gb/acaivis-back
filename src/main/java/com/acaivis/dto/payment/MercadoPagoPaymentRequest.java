package com.acaivis.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record MercadoPagoPaymentRequest(

        @NotBlank
        String paymentMethodId,

        @NotBlank
        String paymentMethodType,

        String token,

        Integer installments,

        @NotBlank
        String payerEmail

) {
}