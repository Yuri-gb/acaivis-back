package com.acaivis.dto.payment;

import java.time.OffsetDateTime;

public record MercadoPagoPaymentResponse(
        String orderId,
        String paymentId,
        String status,
        String statusDetail,
        String qrCode,
        String qrCodeBase64,
        String ticketUrl,
        OffsetDateTime expiresAt
) {
}