package com.acaivis.dto.payment;

import java.time.LocalDateTime;

public record MercadoPagoPaymentResponse(
        String orderId,
        String paymentId,
        String status,
        String statusDetail,
        String qrCode,
        String qrCodeBase64,
        String ticketUrl,
        LocalDateTime expiresAt
) {
}