package com.acaivis.controller;

import com.acaivis.dto.payment.MercadoPagoPaymentRequest;
import com.acaivis.dto.payment.MercadoPagoPaymentResponse;
import com.acaivis.exception.BusinessException;
import com.acaivis.exception.ResourceNotFoundException;
import com.acaivis.model.Order;
import com.acaivis.model.OrderStatus;
import com.acaivis.repository.OrderRepository;
import com.acaivis.service.MercadoPagoService;
import com.acaivis.service.OrderService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final OrderRepository orders;
    private final MercadoPagoService mercadoPago;
    private final OrderService orderService;

    @Value("${mercadopago.webhook-secret:}")
    private String webhookSecret;

    public PaymentController(
            OrderRepository orders,
            MercadoPagoService mercadoPago,
            OrderService orderService
    ) {
        this.orders = orders;
        this.mercadoPago = mercadoPago;
        this.orderService = orderService;
    }

    @PostMapping("/orders/{orderId}")
    @Transactional
    public ResponseEntity<MercadoPagoPaymentResponse> criarPagamento(
            @PathVariable Long orderId,
            @Valid @RequestBody MercadoPagoPaymentRequest request
    ) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Pedido não encontrado: " + orderId
                ));

        if (order.getStatus() == OrderStatus.PAID) {
            throw new BusinessException("Este pedido já está pago.");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Não é possível pagar um pedido cancelado.");
        }

        if (order.getCustomerEmail() == null || order.getCustomerEmail().isBlank()) {
            throw new BusinessException("O pedido precisa ter um e-mail do cliente.");
        }

        MercadoPagoPaymentRequest pagamento = new MercadoPagoPaymentRequest(
                request.paymentMethodId(),
                request.paymentMethodType(),
                request.token(),
                request.installments(),
                order.getCustomerEmail(),
                request.idempotencyKey()
        );

        MercadoPagoPaymentResponse response;
        try {
            response = mercadoPago.criarPagamento(
                    order.getTotal(),
                    order.getTrackingCode(),
                    pagamento
            );
        } catch (RuntimeException exception) {
            orderService.cancelarPagamentoFalho(orderId);
            throw exception;
        }

        order.setMercadoPagoOrderId(response.orderId());
        order.setMercadoPagoPaymentId(response.paymentId());
        order.setPaymentStatus(response.status());
        order.setPaymentStatusDetail(response.statusDetail());
        order.setPaymentExpiresAt(response.expiresAt() == null ? null : response.expiresAt().toLocalDateTime());
        order.setPixQrCode(response.qrCode());
        order.setPixQrCodeBase64(response.qrCodeBase64());
        order.setPixTicketUrl(response.ticketUrl());

        orders.save(order);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/tracking/{trackingCode}")
    @Transactional(readOnly = true)
    public ResponseEntity<MercadoPagoPaymentResponse> status(
            @PathVariable String trackingCode
    ) {
        Order order = orders.findByTrackingCode(trackingCode.trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado"));

        return ResponseEntity.ok(new MercadoPagoPaymentResponse(
                order.getMercadoPagoOrderId(),
                order.getMercadoPagoPaymentId(),
                order.getPaymentStatus(),
                order.getPaymentStatusDetail(),
                order.getPixQrCode(),
                order.getPixQrCodeBase64(),
                order.getPixTicketUrl(),
                order.getPaymentExpiresAt() == null ? null : order.getPaymentExpiresAt().atOffset(ZoneOffset.UTC)
        ));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId
    ) {
        if (!isValidSignature(dataId, xSignature, xRequestId)) {
            return ResponseEntity.status(401).build();
        }

        if (dataId != null && !dataId.isBlank()) {
            orderService.processarWebhookMercadoPago(dataId);
        }

        return ResponseEntity.ok().build();
    }

    private boolean isValidSignature(
            String dataId,
            String xSignature,
            String xRequestId
    ) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return false;
        }

        if (xSignature == null || xRequestId == null || dataId == null) {
            return false;
        }

        String ts = null;
        String v1 = null;

        for (String part : xSignature.split(",")) {
            String[] pair = part.trim().split("=", 2);
            if (pair.length != 2) {
                continue;
            }

            if ("ts".equals(pair[0])) {
                ts = pair[1];
            } else if ("v1".equals(pair[0])) {
                v1 = pair[1];
            }
        }

        if (ts == null || v1 == null) {
            return false;
        }

        String manifest = "id:" + dataId.toLowerCase()
                + ";request-id:" + xRequestId
                + ";ts:" + ts + ";";

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            ));

            byte[] expected = mac.doFinal(
                    manifest.getBytes(StandardCharsets.UTF_8)
            );

            byte[] received = hexToBytes(v1);
            return MessageDigest.isEqual(expected, received);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] hexToBytes(String value) {
        if (value == null || value.length() % 2 != 0) {
            throw new IllegalArgumentException("Assinatura inválida");
        }

        byte[] bytes = new byte[value.length() / 2];

        for (int i = 0; i < value.length(); i += 2) {
            int high = Character.digit(value.charAt(i), 16);
            int low = Character.digit(value.charAt(i + 1), 16);

            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("Assinatura inválida");
            }

            bytes[i / 2] = (byte) ((high << 4) + low);
        }

        return bytes;
    }
}
