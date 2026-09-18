package com.acaivis.dto.payment;

public record MercadoPagoPaymentResponse(

        String orderId,

        String status,

        String statusDetail,

        String qrCode,

        String qrCodeBase64

) {
}