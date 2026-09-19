package com.acaivis.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record MercadoPagoPaymentRequest(
        @NotBlank String paymentMethodId,
        @NotBlank String paymentMethodType,
        String token,
        Integer installments,
        @NotBlank String payerEmail,
        String payerIdentificationType,
        String payerIdentificationNumber,
        String idempotencyKey
) {
    public MercadoPagoPaymentRequest(
            String paymentMethodId,
            String paymentMethodType,
            String token,
            Integer installments,
            String payerEmail,
            String idempotencyKey
    ) {
        this(paymentMethodId, paymentMethodType, token, installments, payerEmail, null, null, idempotencyKey);
    }
}
