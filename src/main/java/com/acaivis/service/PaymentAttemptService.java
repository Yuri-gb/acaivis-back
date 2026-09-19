package com.acaivis.service;

import com.acaivis.dto.OrderRequest;
import com.acaivis.dto.OrderResponse;
import com.acaivis.dto.payment.MercadoPagoCardCheckoutResponse;
import com.acaivis.dto.payment.MercadoPagoPaymentResponse;
import com.acaivis.model.Order;
import com.acaivis.model.PaymentAttempt;
import com.acaivis.repository.PaymentAttemptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class PaymentAttemptService {

    private final PaymentAttemptRepository attempts;
    private final MercadoPagoService mercadoPago;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public PaymentAttemptService(
            PaymentAttemptRepository attempts,
            MercadoPagoService mercadoPago,
            OrderService orderService,
            ObjectMapper objectMapper
    ) {
        this.attempts = attempts;
        this.mercadoPago = mercadoPago;
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void register(
            OrderRequest orderRequest,
            BigDecimal amount,
            MercadoPagoPaymentResponse payment
    ) {
        if (payment.orderId() == null || payment.orderId().isBlank()) {
            throw new IllegalStateException("Mercado Pago não retornou o ID da order.");
        }

        PaymentAttempt attempt = attempts
                .findByMercadoPagoOrderId(payment.orderId())
                .orElseGet(PaymentAttempt::new);

        try {
            attempt.setMercadoPagoOrderId(payment.orderId());
            attempt.setMercadoPagoPaymentId(payment.paymentId());
            attempt.setStatus(normalizeStatus(payment.status()));
            attempt.setStatusDetail(payment.statusDetail());
            attempt.setAmount(amount);
            attempt.setOrderRequestJson(objectMapper.writeValueAsString(orderRequest));
            attempts.save(attempt);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Não foi possível persistir a tentativa de pagamento.",
                    exception
            );
        }
    }

    @Transactional
    public MercadoPagoCardCheckoutResponse processWebhook(String mercadoPagoOrderId) {
        return resolve(mercadoPagoOrderId);
    }

    @Transactional
    public MercadoPagoCardCheckoutResponse status(String mercadoPagoOrderId) {
        return resolve(mercadoPagoOrderId);
    }

    private MercadoPagoCardCheckoutResponse resolve(String mercadoPagoOrderId) {
        if (mercadoPagoOrderId == null || mercadoPagoOrderId.isBlank()) {
            return new MercadoPagoCardCheckoutResponse(
                    emptyPayment(),
                    null
            );
        }

        PaymentAttempt attempt = attempts
                .findByMercadoPagoOrderId(mercadoPagoOrderId)
                .orElse(null);

        if (attempt == null) {
            return new MercadoPagoCardCheckoutResponse(emptyPayment(), null);
        }

        Map<String, Object> mpOrder = mercadoPago.buscarOrder(mercadoPagoOrderId);
        if (mpOrder.isEmpty()) {
            return responseForAttempt(attempt);
        }

        MercadoPagoPaymentResponse payment = mercadoPago.toPaymentResponse(mpOrder);

        attempt.setMercadoPagoPaymentId(payment.paymentId());
        attempt.setStatus(normalizeStatus(payment.status()));
        attempt.setStatusDetail(payment.statusDetail());

        if (isApproved(payment.status(), string(mpOrder.get("status")))) {
            if (attempt.getResolvedOrderId() == null) {
                resolveApprovedAttempt(attempt);
            }
        } else if (isFinalFailure(payment.status(), string(mpOrder.get("status")))) {
            attempt.setStatus("FAILED");
        } else {
            attempt.setStatus("PROCESSING");
        }

        attempts.save(attempt);
        return responseFor(attempt, payment);
    }

    private void resolveApprovedAttempt(PaymentAttempt attempt) {
        try {
            OrderRequest request = objectMapper.readValue(
                    attempt.getOrderRequestJson(),
                    OrderRequest.class
            );

            OrderResponse created = orderService.create(request);

            Order order = orderService.getEntity(created.id());
            order.setMercadoPagoOrderId(attempt.getMercadoPagoOrderId());
            order.setMercadoPagoPaymentId(attempt.getMercadoPagoPaymentId());
            order.setPaymentStatus("processed");
            order.setPaymentStatusDetail("accredited");
            order.setPaymentConfirmed(true);
            order.setStatus(com.acaivis.model.OrderStatus.PAID);

            orderService.saveApprovedPayment(order);
            attempt.setResolvedOrderId(order.getId());
            attempt.setStatus("RESOLVED");
        } catch (com.acaivis.exception.BusinessException exception) {
            try {
                mercadoPago.reembolsarOrder(attempt.getMercadoPagoOrderId());
                attempt.setStatus("REFUNDED");
                attempt.setStatusDetail("order_creation_failed_refunded");
            } catch (RuntimeException refundException) {
                attempt.setStatus("COMPENSATION_REQUIRED");
                attempt.setStatusDetail("order_creation_failed_refund_failed");
            }
        } catch (Exception exception) {
            attempt.setStatus("COMPENSATION_REQUIRED");
            attempt.setStatusDetail("order_creation_failed");
        }
    }

    private MercadoPagoCardCheckoutResponse responseForAttempt(PaymentAttempt attempt) {
        MercadoPagoPaymentResponse payment = new MercadoPagoPaymentResponse(
                attempt.getMercadoPagoOrderId(),
                attempt.getMercadoPagoPaymentId(),
                attempt.getStatus().toLowerCase(),
                attempt.getStatusDetail(),
                null,
                null,
                null,
                null
        );
        return responseFor(attempt, payment);
    }

    private MercadoPagoCardCheckoutResponse responseFor(
            PaymentAttempt attempt,
            MercadoPagoPaymentResponse payment
    ) {
        OrderResponse order = attempt.getResolvedOrderId() == null
                ? null
                : orderService.find(attempt.getResolvedOrderId());

        return new MercadoPagoCardCheckoutResponse(payment, order);
    }

    private boolean isApproved(String paymentStatus, String orderStatus) {
        return "processed".equalsIgnoreCase(paymentStatus)
                || "approved".equalsIgnoreCase(paymentStatus)
                || "accredited".equalsIgnoreCase(paymentStatus)
                || "processed".equalsIgnoreCase(orderStatus);
    }

    private boolean isFinalFailure(String paymentStatus, String orderStatus) {
        return "failed".equalsIgnoreCase(paymentStatus)
                || "canceled".equalsIgnoreCase(paymentStatus)
                || "cancelled".equalsIgnoreCase(paymentStatus)
                || "expired".equalsIgnoreCase(paymentStatus)
                || "refunded".equalsIgnoreCase(paymentStatus)
                || "failed".equalsIgnoreCase(orderStatus)
                || "canceled".equalsIgnoreCase(orderStatus)
                || "cancelled".equalsIgnoreCase(orderStatus)
                || "expired".equalsIgnoreCase(orderStatus)
                || "refunded".equalsIgnoreCase(orderStatus);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "PROCESSING";
        }
        return status.toUpperCase();
    }

    private String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private MercadoPagoPaymentResponse emptyPayment() {
        return new MercadoPagoPaymentResponse(
                null, null, "processing", null, null, null, null, null
        );
    }
}
